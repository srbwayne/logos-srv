package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReactivateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-reactivate-c1b")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReactivateExternalSubjectOwnershipPostgresTest {
    @Autowired ReactivateExternalSubjectOwnershipUseCase reactivate;
    @Autowired DisableExternalSubjectOwnershipUseCase disable;
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;

    @BeforeEach void authorize() {
        when(operatorContext.authorizeForNamespace("lifeos"))
                .thenReturn(new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD,
                        "verified-workload-reactivate", "lifeos"));
    }

    private ExternalSubjectReference createDisabledIdentity(String verification) {
        String email = "reactivate-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Reactivate " + UUID.randomUUID()));
        var reference = new ExternalSubjectReference("lifeos", "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        long version = 0;
        if (!verification.equals("UNVERIFIED")) {
            verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, version,
                    "initial-review", "verify-case", "initial verification"));
            version++;
        }
        if (verification.equals("INVALIDATED")) {
            invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(reference, version,
                    "invalidate-review", "invalidate-case", "verification withdrawn"));
            version++;
        }
        disable.disable(new DisableExternalSubjectOwnershipCommand(reference, version, "administrative suspension"));
        return reference;
    }

    private ReactivateExternalSubjectOwnershipCommand command(ExternalSubjectReference reference, long version) {
        return new ReactivateExternalSubjectOwnershipCommand(reference, version, "  approved restoration  ");
    }

    @Test void reactivatesAllSupportedVerificationStatesWithCompleteHistoryAndPreservedIdentity() {
        for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
            var reference = createDisabledIdentity(verification);
            var before = identity(reference);
            var previousHistory = history(reference);
            long version = ((Number) before.get("ownership_version")).longValue();

            reactivate.reactivate(command(reference, version));

            var after = identity(reference);
            assertThat(after).containsEntry("id", before.get("id"))
                    .containsEntry("namespace", before.get("namespace"))
                    .containsEntry("external_id", before.get("external_id"))
                    .containsEntry("jogador_id", before.get("jogador_id"))
                    .containsEntry("identity_class", "EXTERNAL")
                    .containsEntry("ownership_status", "ACTIVE")
                    .containsEntry("verification_status", verification)
                    .containsEntry("ownership_version", version + 1);

            var event = jdbc.queryForMap("""
                    SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,
                           previous_target_jogador_id,new_target_jogador_id,
                           previous_ownership_status,new_ownership_status,
                           previous_verification_status,new_verification_status,
                           provenance,actor_type,actor_id,evidence_type,evidence_reference,
                           reason,effective_at,recorded_at
                    FROM progression_subject_ownership_history h
                    JOIN progression_subject_identity i ON i.id=h.identity_id
                    WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REACTIVATED'
                    """, reference.namespace(), reference.externalId());
            assertThat(event).containsEntry("aggregate_version", version + 1)
                    .containsEntry("event_type", "OWNERSHIP_REACTIVATED")
                    .containsEntry("previous_identity_class", "EXTERNAL")
                    .containsEntry("new_identity_class", "EXTERNAL")
                    .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                    .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                    .containsEntry("previous_ownership_status", "DISABLED")
                    .containsEntry("new_ownership_status", "ACTIVE")
                    .containsEntry("previous_verification_status", verification)
                    .containsEntry("new_verification_status", verification)
                    .containsEntry("provenance", "LOGOS_OPERATOR_ACTION")
                    .containsEntry("actor_type", "WORKLOAD_OPERATOR")
                    .containsEntry("actor_id", "verified-workload-reactivate")
                    .containsEntry("evidence_type", null)
                    .containsEntry("evidence_reference", null)
                    .containsEntry("reason", "approved restoration");
            assertThat(event.get("effective_at")).isNotNull();
            assertThat(event.get("recorded_at")).isNotNull();
            var afterHistory = history(reference);
            assertThat(afterHistory).hasSize(previousHistory.size() + 1);
            assertThat(afterHistory.subList(0, previousHistory.size())).isEqualTo(previousHistory);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM progression_subject_ownership_history h
                    JOIN progression_subject_identity i ON i.id=h.identity_id
                    WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REACTIVATED'
                    """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        }
    }

    @Test void activeReplayIgnoresStaleVersionWithoutChangingIdentityOrHistoryAndValidatesReason() {
        var reference = createDisabledIdentity("VERIFIED");
        reactivate.reactivate(command(reference, 2));
        var identityBefore = identity(reference);
        var historyBefore = history(reference);

        reactivate.reactivate(new ReactivateExternalSubjectOwnershipCommand(reference, 0, "different replay reason"));

        assertThat(identity(reference)).isEqualTo(identityBefore);
        assertThat(history(reference)).isEqualTo(historyBefore);
        assertThatThrownBy(() -> reactivate.reactivate(
                new ReactivateExternalSubjectOwnershipCommand(reference, 0, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(identity(reference)).isEqualTo(identityBefore);
        assertThat(history(reference)).isEqualTo(historyBefore);
    }

    @Test void historyFailureRollsBackOwnershipStatusAndVersion() {
        var reference = createDisabledIdentity("INVALIDATED");
        jdbc.execute("CREATE FUNCTION reject_reactivate_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_REACTIVATED' THEN RAISE EXCEPTION 'intentional reactivation history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_reactivate_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_reactivate_history()");
        try {
            assertThatThrownBy(() -> reactivate.reactivate(command(reference, 3))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_reactivate_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_reactivate_history()");
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "DISABLED")
                .containsEntry("ownership_version", 3L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REACTIVATED'
                """, Integer.class, reference.namespace(), reference.externalId())).isZero();
    }

    @Test void concurrentEquivalentRequestsSerializeToOneMutationAndOneHistoryEvent() throws Exception {
        var reference = createDisabledIdentity("UNVERIFIED");
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { ready.countDown(); start.await(); reactivate.reactivate(command(reference, 1)); return null; });
            var second = pool.submit(() -> { ready.countDown(); start.await(); reactivate.reactivate(command(reference, 1)); return null; });
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "UNVERIFIED").containsEntry("ownership_version", 2L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REACTIVATED'
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    private Map<String, Object> identity(ExternalSubjectReference reference) {
        return jdbc.queryForMap("""
                SELECT id,namespace,external_id,jogador_id,identity_class,ownership_status,
                       verification_status,ownership_version
                FROM progression_subject_identity WHERE namespace=? AND external_id=?
                """, reference.namespace(), reference.externalId());
    }

    private List<Map<String, Object>> history(ExternalSubjectReference reference) {
        return jdbc.queryForList("""
                SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,
                       previous_target_jogador_id,new_target_jogador_id,
                       previous_ownership_status,new_ownership_status,
                       previous_verification_status,new_verification_status,
                       provenance,actor_type,actor_id,evidence_type,evidence_reference,
                       reason,effective_at,recorded_at
                FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? ORDER BY h.aggregate_version
                """, reference.namespace(), reference.externalId());
    }
}
