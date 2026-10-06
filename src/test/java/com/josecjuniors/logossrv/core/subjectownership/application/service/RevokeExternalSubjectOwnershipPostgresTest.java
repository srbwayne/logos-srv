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
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipUseCase;
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
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-revoke-c1b")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevokeExternalSubjectOwnershipPostgresTest {
    @Autowired RevokeExternalSubjectOwnershipUseCase revoke;
    @Autowired DisableExternalSubjectOwnershipUseCase disable;
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired ReactivateExternalSubjectOwnershipUseCase reactivate;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;

    @BeforeEach void authorize() {
        when(operatorContext.authorizeForNamespace("lifeos"))
                .thenReturn(new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD,
                        "verified-workload-revoke", "lifeos"));
    }

    private ExternalSubjectReference createIdentity(String verification, boolean disabled) {
        String email = "revoke-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Revoke " + UUID.randomUUID()));
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
        if (disabled) {
            disable.disable(new DisableExternalSubjectOwnershipCommand(reference, version, "administrative suspension"));
            version++;
        }
        return reference;
    }

    private RevokeExternalSubjectOwnershipCommand command(ExternalSubjectReference reference, long version) {
        return new RevokeExternalSubjectOwnershipCommand(reference, version, "  approved termination  ");
    }

    @Test void revokesAllSixEligibleShapesWithCompleteImmutableHistory() {
        for (String ownership : List.of("ACTIVE", "DISABLED")) {
            for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
                var reference = createIdentity(verification, ownership.equals("DISABLED"));
                var before = identity(reference);
                var previousHistory = history(reference);
                long version = ((Number) before.get("ownership_version")).longValue();
                revoke.revoke(command(reference, version));

                var after = identity(reference);
                assertThat(after).containsEntry("id", before.get("id"))
                        .containsEntry("namespace", before.get("namespace"))
                        .containsEntry("external_id", before.get("external_id"))
                        .containsEntry("jogador_id", before.get("jogador_id"))
                        .containsEntry("identity_class", "EXTERNAL")
                        .containsEntry("ownership_status", "REVOKED")
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
                        WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVOKED'
                        """, reference.namespace(), reference.externalId());
                assertThat(event).containsEntry("aggregate_version", version + 1)
                        .containsEntry("event_type", "OWNERSHIP_REVOKED")
                        .containsEntry("previous_identity_class", "EXTERNAL")
                        .containsEntry("new_identity_class", "EXTERNAL")
                        .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                        .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                        .containsEntry("previous_ownership_status", ownership)
                        .containsEntry("new_ownership_status", "REVOKED")
                        .containsEntry("previous_verification_status", verification)
                        .containsEntry("new_verification_status", verification)
                        .containsEntry("provenance", "LOGOS_OPERATOR_ACTION")
                        .containsEntry("actor_type", "WORKLOAD_OPERATOR")
                        .containsEntry("actor_id", "verified-workload-revoke")
                        .containsEntry("evidence_type", null)
                        .containsEntry("evidence_reference", null)
                        .containsEntry("reason", "approved termination");
                assertThat(event.get("effective_at")).isNotNull();
                assertThat(event.get("recorded_at")).isNotNull();
                var afterHistory = history(reference);
                assertThat(afterHistory).hasSize(previousHistory.size() + 1);
                assertThat(afterHistory.subList(0, previousHistory.size())).isEqualTo(previousHistory);
            }
        }
    }

    @Test void revokedReplayIgnoresStaleVersionWithoutChangingRowHistoryOrOriginalReason() {
        var reference = createIdentity("VERIFIED", false);
        revoke.revoke(command(reference, 1));
        var identityBefore = identity(reference);
        var historyBefore = history(reference);
        revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "new replay reason"));
        assertThat(identity(reference)).isEqualTo(identityBefore);
        assertThat(history(reference)).isEqualTo(historyBefore);
        assertThatThrownBy(() -> revoke.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(identity(reference)).isEqualTo(identityBefore);
        assertThat(history(reference)).isEqualTo(historyBefore);
    }

    @Test void historyFailureRollsBackOwnershipAndVersion() {
        var reference = createIdentity("INVALIDATED", false);
        jdbc.execute("CREATE FUNCTION reject_revoke_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_REVOKED' THEN RAISE EXCEPTION 'intentional revoke history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_revoke_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_revoke_history()");
        try {
            assertThatThrownBy(() -> revoke.revoke(command(reference, 2))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_revoke_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_revoke_history()");
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "ACTIVE")
                .containsEntry("ownership_version", 2L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVOKED'
                """, Integer.class, reference.namespace(), reference.externalId())).isZero();
    }

    @Test void concurrentEquivalentRequestsCreateOneRevocationAndOneVersionIncrement() throws Exception {
        var reference = createIdentity("UNVERIFIED", false);
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { ready.countDown(); start.await(); revoke.revoke(command(reference, 0)); return null; });
            var second = pool.submit(() -> { ready.countDown(); start.await(); revoke.revoke(command(reference, 0)); return null; });
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "REVOKED")
                .containsEntry("verification_status", "UNVERIFIED").containsEntry("ownership_version", 1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history h
                JOIN progression_subject_identity i ON i.id=h.identity_id
                WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVOKED'
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    @Test void reactivationContinuesToRejectRevokedBinding() {
        var reference = createIdentity("VERIFIED", false);
        revoke.revoke(command(reference, 1));
        assertThatThrownBy(() -> reactivate.reactivate(
                new ReactivateExternalSubjectOwnershipCommand(reference, 2, "attempt restore")))
                .isInstanceOf(RuntimeException.class);
        assertThat(identity(reference)).containsEntry("ownership_status", "REVOKED")
                .containsEntry("ownership_version", 2L);
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
