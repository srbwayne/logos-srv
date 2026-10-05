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
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-disable-c1b")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DisableExternalSubjectOwnershipPostgresTest {
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
                        "verified-workload-disable", "lifeos"));
    }

    private ExternalSubjectReference createIdentity() {
        String email = "disable-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Disable " + UUID.randomUUID()));
        var reference = new ExternalSubjectReference("lifeos", "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        return reference;
    }

    private long moveTo(ExternalSubjectReference reference, String verificationStatus) {
        if (verificationStatus.equals("UNVERIFIED")) return 0L;
        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 0,
                "initial-review", "verify-case", "initial verification"));
        if (verificationStatus.equals("VERIFIED")) return 1L;
        invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(reference, 1,
                "invalidate-review", "invalidate-case", "verification withdrawn"));
        return 2L;
    }

    private DisableExternalSubjectOwnershipCommand command(ExternalSubjectReference reference, long version) {
        return new DisableExternalSubjectOwnershipCommand(reference, version, "  administrative suspension  ");
    }

    @Test void disablesAllSupportedVerificationStatesWithCompleteImmutableHistoryAndNoSideEffects() {
        for (String verification : List.of("UNVERIFIED", "VERIFIED", "INVALIDATED")) {
            var reference = createIdentity();
            long version = moveTo(reference, verification);
            var before = identity(reference);
            var previousHistory = history(reference);
            var trustBefore = trustStateCounts();
            UUID executionId = insertExecutionSnapshot(reference);
            var executionBefore = jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id=?", executionId);

            disable.disable(command(reference, version));

            var after = identity(reference);
            assertThat(after).containsEntry("id", before.get("id")).containsEntry("namespace", reference.namespace())
                    .containsEntry("external_id", reference.externalId()).containsEntry("jogador_id", before.get("jogador_id"))
                    .containsEntry("identity_class", "EXTERNAL").containsEntry("ownership_status", "DISABLED")
                    .containsEntry("verification_status", verification).containsEntry("ownership_version", version + 1);

            var event = jdbc.queryForMap("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_DISABLED'", reference.namespace(), reference.externalId());
            assertThat(event).containsEntry("aggregate_version", version + 1)
                    .containsEntry("event_type", "OWNERSHIP_DISABLED")
                    .containsEntry("previous_identity_class", "EXTERNAL").containsEntry("new_identity_class", "EXTERNAL")
                    .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                    .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                    .containsEntry("previous_ownership_status", "ACTIVE").containsEntry("new_ownership_status", "DISABLED")
                    .containsEntry("previous_verification_status", verification).containsEntry("new_verification_status", verification)
                    .containsEntry("provenance", "LOGOS_OPERATOR_ACTION").containsEntry("actor_type", "WORKLOAD_OPERATOR")
                    .containsEntry("actor_id", "verified-workload-disable").containsEntry("evidence_type", null)
                    .containsEntry("evidence_reference", null).containsEntry("reason", "administrative suspension");
            assertThat(event.get("effective_at")).isNotNull();
            assertThat(event.get("recorded_at")).isNotNull();
            assertThat(history(reference)).hasSize(previousHistory.size() + 1);
            assertThat(history(reference).subList(0, previousHistory.size())).isEqualTo(previousHistory);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_DISABLED'", Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
            assertThat(trustStateCounts()).isEqualTo(trustBefore);
            assertThat(jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id=?", executionId)).isEqualTo(executionBefore);
        }
    }

    @Test void disabledReplayIgnoresStaleVersionWithoutChangingHistoryOrIdentity() {
        var reference = createIdentity();
        disable.disable(command(reference, 0));
        var before = identity(reference);
        var historyBefore = history(reference);

        disable.disable(new DisableExternalSubjectOwnershipCommand(reference, 0, "different replay reason"));

        assertThat(identity(reference)).isEqualTo(before);
        assertThat(history(reference)).isEqualTo(historyBefore);
        assertThatThrownBy(() -> disable.disable(new DisableExternalSubjectOwnershipCommand(reference, 0, " ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(history(reference)).isEqualTo(historyBefore);
    }

    @Test void historyFailureRollsBackOwnershipStatusAndVersion() {
        var reference = createIdentity();
        jdbc.execute("CREATE FUNCTION reject_disable_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_DISABLED' THEN RAISE EXCEPTION 'intentional disable history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_disable_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_disable_history()");
        try {
            assertThatThrownBy(() -> disable.disable(command(reference, 0))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_disable_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_disable_history()");
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "ACTIVE").containsEntry("ownership_version", 0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_DISABLED'", Integer.class, reference.namespace(), reference.externalId())).isZero();
    }

    @Test void concurrentEquivalentDisableRequestsProduceOneMutationAndHistoryEvent() throws Exception {
        var reference = createIdentity();
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { ready.countDown(); start.await(); disable.disable(command(reference, 0)); return null; });
            var second = pool.submit(() -> { ready.countDown(); start.await(); disable.disable(command(reference, 0)); return null; });
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(identity(reference)).containsEntry("ownership_status", "DISABLED")
                .containsEntry("verification_status", "UNVERIFIED").containsEntry("ownership_version", 1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_DISABLED'", Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    private Map<String, Object> identity(ExternalSubjectReference reference) {
        return jdbc.queryForMap("SELECT id,namespace,external_id,jogador_id,identity_class,ownership_status,verification_status,ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", reference.namespace(), reference.externalId());
    }

    private List<Map<String, Object>> history(ExternalSubjectReference reference) {
        return jdbc.queryForList("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? ORDER BY h.aggregate_version", reference.namespace(), reference.externalId());
    }

    private Map<String, Long> trustStateCounts() {
        return Map.of(
                "authorization_grant", jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class),
                "workload_principal", jdbc.queryForObject("SELECT count(*) FROM workload_principal", Long.class),
                "workload_signing_key", jdbc.queryForObject("SELECT count(*) FROM workload_signing_key", Long.class),
                "workload_trust_audit_event", jdbc.queryForObject("SELECT count(*) FROM workload_trust_audit_event", Long.class));
    }

    private UUID insertExecutionSnapshot(ExternalSubjectReference reference) {
        UUID configurationId = UUID.randomUUID();
        UUID configurationVersionId = UUID.randomUUID();
        UUID skillPolicyId = UUID.randomUUID();
        UUID skillPolicyVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition (id, logical_key) VALUES (?, ?)", configurationId, "disable-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_configuration_version (id, definition_id, revision, base_xp, base_stress) VALUES (?, ?, 1, 1, 0)", configurationVersionId, configurationId);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id=? WHERE id=?", configurationVersionId, configurationId);
        jdbc.update("INSERT INTO progression_skill_policy (id, logical_key) VALUES (?, ?)", skillPolicyId, "disable-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_skill_policy_version (id, policy_id, revision) VALUES (?, ?, 1)", skillPolicyVersionId, skillPolicyId);
        jdbc.update("UPDATE progression_skill_policy SET current_version_id=? WHERE id=?", skillPolicyVersionId, skillPolicyId);
        UUID executionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO progression_external_execution
                    (id, source_system, idempotency_key, request_fingerprint, response_json, request_json,
                     processing_status, attempt_count, last_error, subject_namespace, subject_external_id,
                     configuration_key, requested_revision, configuration_version_id,
                     skill_policy_version_id, created_at)
                VALUES (?, 'disable-snapshot', ?, 'fingerprint', '{\"result\":\"stable\"}', ?,
                        'COMPLETED', 1, NULL, ?, ?, 'reading', 1, ?, ?, CURRENT_TIMESTAMP)
                """, executionId, "idempotency-" + UUID.randomUUID(),
                "{\"subject\":{\"namespace\":\"" + reference.namespace() + "\",\"externalId\":\"" + reference.externalId() + "\"}}",
                reference.namespace(), reference.externalId(), configurationVersionId, skillPolicyVersionId);
        return executionId;
    }
}
