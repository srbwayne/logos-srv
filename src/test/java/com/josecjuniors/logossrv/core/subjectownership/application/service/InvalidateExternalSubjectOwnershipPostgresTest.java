package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReverifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReverifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
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
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-verify-c1b")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InvalidateExternalSubjectOwnershipPostgresTest {
    @Autowired InvalidateExternalSubjectOwnershipUseCase invalidate;
    @Autowired ReverifyExternalSubjectOwnershipUseCase reverify;
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;

    @BeforeEach void authorize() {
        when(operatorContext.authorizeForNamespace("lifeos"))
                .thenReturn(new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "verified-workload-invalidate", "lifeos"));
    }

    private ExternalSubjectReference createVerifiedIdentity() {
        String email = "invalidate-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Invalidate " + UUID.randomUUID()));
        var reference = new ExternalSubjectReference("lifeos", "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        verify.verify(new VerifyExternalSubjectOwnershipCommand(reference, 0, "operator-review", "verify-case", "verified before invalidation"));
        return reference;
    }

    private InvalidateExternalSubjectOwnershipCommand command(ExternalSubjectReference ref, long version) {
        return new InvalidateExternalSubjectOwnershipCommand(ref, version, " operator-review ",
                " invalidate-case-" + UUID.randomUUID() + " ", " verification no longer supported ");
    }

    private ReverifyExternalSubjectOwnershipCommand reverifyCommand(ExternalSubjectReference ref, long version) {
        return new ReverifyExternalSubjectOwnershipCommand(ref, version, " reverify-review ",
                " reverify-case-" + UUID.randomUUID() + " ", " new evidence reviewed ");
    }

    private ExternalSubjectReference createInvalidatedIdentity() {
        var reference = createVerifiedIdentity();
        invalidate.invalidate(command(reference, 1));
        return reference;
    }

    @Test void invalidatesWithOneCompleteHistorySnapshotAndPreservesTrustAndExecutionState() {
        var ref = createVerifiedIdentity();
        var before = jdbc.queryForMap("SELECT id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        UUID executionId = insertExecutionSnapshot(ref);
        var executionBefore = jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId);
        var trustBefore = trustStateCounts();

        invalidate.invalidate(command(ref, 1));

        var after = jdbc.queryForMap("SELECT id, namespace, external_id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        assertThat(after).containsEntry("id", before.get("id")).containsEntry("namespace", ref.namespace())
                .containsEntry("external_id", ref.externalId()).containsEntry("jogador_id", before.get("jogador_id"))
                .containsEntry("identity_class", "EXTERNAL").containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "INVALIDATED").containsEntry("ownership_version", 2L);

        var history = jdbc.queryForMap("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFICATION_INVALIDATED'", ref.namespace(), ref.externalId());
        assertThat(history).containsEntry("aggregate_version", 2L).containsEntry("event_type", "OWNERSHIP_VERIFICATION_INVALIDATED")
                .containsEntry("previous_identity_class", "EXTERNAL").containsEntry("new_identity_class", "EXTERNAL")
                .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                .containsEntry("previous_ownership_status", "ACTIVE").containsEntry("new_ownership_status", "ACTIVE")
                .containsEntry("previous_verification_status", "VERIFIED").containsEntry("new_verification_status", "INVALIDATED")
                .containsEntry("provenance", "LOGOS_OPERATOR_ACTION").containsEntry("actor_type", "WORKLOAD_OPERATOR")
                .containsEntry("actor_id", "verified-workload-invalidate").containsEntry("evidence_type", "operator-review")
                .containsEntry("reason", "verification no longer supported");
        assertThat(history.get("evidence_reference").toString()).startsWith("invalidate-case-");
        assertThat(history.get("effective_at")).isNotNull();
        assertThat(history.get("recorded_at")).isNotNull();
        assertThat(jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId)).isEqualTo(executionBefore);
        assertThat(trustStateCounts()).isEqualTo(trustBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFICATION_INVALIDATED'", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
    }

    @Test void repeatedInvalidationIsNoOpEvenWithStaleVersion() {
        var ref = createVerifiedIdentity();
        invalidate.invalidate(command(ref, 1));
        invalidate.invalidate(new InvalidateExternalSubjectOwnershipCommand(ref, 0, "ticket", "different", "replay"));
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("INVALIDATED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFICATION_INVALIDATED'", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
    }

    @Test void historyFailureRollsBackCurrentRow() {
        var ref = createVerifiedIdentity();
        jdbc.execute("CREATE FUNCTION reject_invalidate_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_VERIFICATION_INVALIDATED' THEN RAISE EXCEPTION 'intentional invalidation history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_invalidate_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_invalidate_history()");
        try {
            assertThatThrownBy(() -> invalidate.invalidate(command(ref, 1))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_invalidate_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_invalidate_history()");
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("VERIFIED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFICATION_INVALIDATED'", Integer.class, ref.namespace(), ref.externalId())).isZero();
    }

    @Test void concurrentInvalidationsProduceOneMutationAndOneHistoryEvent() throws Exception {
        var ref = createVerifiedIdentity();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { start.await(); invalidate.invalidate(command(ref, 1)); return null; });
            var second = pool.submit(() -> { start.await(); invalidate.invalidate(command(ref, 1)); return null; });
            start.countDown();
            first.get(); second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("INVALIDATED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFICATION_INVALIDATED'", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
    }

    @Test void reverifiesWithOneCompleteHistoryEventAndPreservesPriorHistoryTrustAndExecution() {
        var ref = createInvalidatedIdentity();
        var before = jdbc.queryForMap("SELECT id, namespace, external_id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        UUID executionId = insertExecutionSnapshot(ref);
        var executionBefore = jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId);
        var trustBefore = trustStateCounts();
        var priorHistoryBefore = priorHistoryForReverification(ref);

        reverify.reverify(reverifyCommand(ref, 2));

        var after = jdbc.queryForMap("SELECT id, namespace, external_id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        assertThat(after).containsEntry("id", before.get("id")).containsEntry("namespace", ref.namespace())
                .containsEntry("external_id", ref.externalId()).containsEntry("jogador_id", before.get("jogador_id"))
                .containsEntry("identity_class", "EXTERNAL").containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "VERIFIED").containsEntry("ownership_version", 3L);

        var history = jdbc.queryForMap("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVERIFIED'", ref.namespace(), ref.externalId());
        assertThat(history).containsEntry("aggregate_version", 3L).containsEntry("event_type", "OWNERSHIP_REVERIFIED")
                .containsEntry("previous_identity_class", "EXTERNAL").containsEntry("new_identity_class", "EXTERNAL")
                .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                .containsEntry("previous_ownership_status", "ACTIVE").containsEntry("new_ownership_status", "ACTIVE")
                .containsEntry("previous_verification_status", "INVALIDATED").containsEntry("new_verification_status", "VERIFIED")
                .containsEntry("provenance", "LOGOS_OPERATOR_ACTION").containsEntry("actor_type", "WORKLOAD_OPERATOR")
                .containsEntry("actor_id", "verified-workload-invalidate").containsEntry("evidence_type", "reverify-review")
                .containsEntry("reason", "new evidence reviewed");
        assertThat(history.get("evidence_reference").toString()).startsWith("reverify-case-");
        assertThat(history.get("effective_at")).isNotNull();
        assertThat(history.get("recorded_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVERIFIED'", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
        assertThat(priorHistoryForReverification(ref)).isEqualTo(priorHistoryBefore);
        assertThat(trustStateCounts()).isEqualTo(trustBefore);
        assertThat(jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId)).isEqualTo(executionBefore);
    }

    @Test void verifiedReverificationReplayDoesNotIncrementVersionAppendHistoryOrReplaceEvidence() {
        var ref = createInvalidatedIdentity();
        reverify.reverify(reverifyCommand(ref, 2));
        var eventBefore = reverificationHistory(ref);

        reverify.reverify(new ReverifyExternalSubjectOwnershipCommand(ref, 0, "new-type", "new-ref", "replay"));

        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("VERIFIED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(3L);
        assertThat(reverificationHistory(ref)).isEqualTo(eventBefore);
        assertThatThrownBy(() -> reverify.reverify(new ReverifyExternalSubjectOwnershipCommand(ref, 0, " ", "ref", "reason"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(reverificationHistory(ref)).isEqualTo(eventBefore);
    }

    @Test void reverificationHistoryFailureRollsBackCurrentStateAndVersion() {
        var ref = createInvalidatedIdentity();
        jdbc.execute("CREATE FUNCTION reject_reverify_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_REVERIFIED' THEN RAISE EXCEPTION 'intentional reverification history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_reverify_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_reverify_history()");
        try {
            assertThatThrownBy(() -> reverify.reverify(reverifyCommand(ref, 2))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_reverify_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_reverify_history()");
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("INVALIDATED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(2L);
        assertThat(reverificationHistory(ref)).isEmpty();
    }

    @Test void concurrentEquivalentReverificationsProduceOneMutationAndHistoryEvent() throws Exception {
        var ref = createInvalidatedIdentity();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { start.await(); reverify.reverify(reverifyCommand(ref, 2)); return null; });
            var second = pool.submit(() -> { start.await(); reverify.reverify(reverifyCommand(ref, 2)); return null; });
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("VERIFIED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(3L);
        assertThat(reverificationHistory(ref)).hasSize(1);
    }

    private java.util.List<java.util.Map<String, Object>> priorHistoryForReverification(ExternalSubjectReference ref) {
        return jdbc.queryForList("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type <> 'OWNERSHIP_REVERIFIED' ORDER BY h.aggregate_version", ref.namespace(), ref.externalId());
    }

    private java.util.List<java.util.Map<String, Object>> reverificationHistory(ExternalSubjectReference ref) {
        return jdbc.queryForList("SELECT aggregate_version,event_type,previous_verification_status,new_verification_status,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_REVERIFIED' ORDER BY h.aggregate_version", ref.namespace(), ref.externalId());
    }

    private Map<String, Long> trustStateCounts() {
        return Map.of(
                "authorization_grant", jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class),
                "workload_principal", jdbc.queryForObject("SELECT count(*) FROM workload_principal", Long.class),
                "workload_signing_key", jdbc.queryForObject("SELECT count(*) FROM workload_signing_key", Long.class),
                "workload_trust_audit_event", jdbc.queryForObject("SELECT count(*) FROM workload_trust_audit_event", Long.class));
    }

    private UUID insertExecutionSnapshot(ExternalSubjectReference ref) {
        UUID configurationId = UUID.randomUUID();
        UUID configurationVersionId = UUID.randomUUID();
        UUID skillPolicyId = UUID.randomUUID();
        UUID skillPolicyVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition (id, logical_key) VALUES (?, ?)", configurationId, "invalidate-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_configuration_version (id, definition_id, revision, base_xp, base_stress) VALUES (?, ?, 1, 1, 0)", configurationVersionId, configurationId);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", configurationVersionId, configurationId);
        jdbc.update("INSERT INTO progression_skill_policy (id, logical_key) VALUES (?, ?)", skillPolicyId, "invalidate-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_skill_policy_version (id, policy_id, revision) VALUES (?, ?, 1)", skillPolicyVersionId, skillPolicyId);
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?", skillPolicyVersionId, skillPolicyId);
        UUID executionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO progression_external_execution
                    (id, source_system, idempotency_key, request_fingerprint, response_json, request_json,
                     processing_status, attempt_count, last_error, subject_namespace, subject_external_id,
                     configuration_key, requested_revision, configuration_version_id,
                     skill_policy_version_id, created_at)
                VALUES (?, 'invalidate-snapshot', ?, 'fingerprint', '{\"result\":\"stable\"}', ?,
                        'COMPLETED', 1, NULL, ?, ?, 'reading', 1, ?, ?, CURRENT_TIMESTAMP)
                """, executionId, "idempotency-" + UUID.randomUUID(),
                "{\"subject\":{\"namespace\":\"" + ref.namespace() + "\",\"externalId\":\"" + ref.externalId() + "\"}}",
                ref.namespace(), ref.externalId(), configurationVersionId, skillPolicyVersionId);
        return executionId;
    }
}
