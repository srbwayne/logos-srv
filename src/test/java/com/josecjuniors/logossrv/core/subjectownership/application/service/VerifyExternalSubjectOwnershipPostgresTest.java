package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipUseCase;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
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
class VerifyExternalSubjectOwnershipPostgresTest {
    @Autowired VerifyExternalSubjectOwnershipUseCase verify;
    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;
    @MockBean SubjectOwnershipOperatorContext operatorContext;

    @BeforeEach void authorize() {
        when(operatorContext.authorizeForNamespace("lifeos"))
                .thenReturn(new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "verified-workload-1", "lifeos"));
    }

    private ExternalSubjectReference createIdentity() {
        String email = "verify-" + UUID.randomUUID() + "@example.test";
        registration.register(new RegistrationCommand(email, "password", "Verify " + UUID.randomUUID()));
        ExternalSubjectReference reference = new ExternalSubjectReference("lifeos", "external-" + UUID.randomUUID());
        provisioning.provision(email, reference);
        return reference;
    }

    private VerifyExternalSubjectOwnershipCommand command(ExternalSubjectReference ref, long version) {
        return new VerifyExternalSubjectOwnershipCommand(ref, version, "operator-review", "case-" + UUID.randomUUID(), "evidence reviewed");
    }

    @Test void verifiesAndWritesOneCompleteHistorySnapshotWithoutChangingTarget() {
        var ref = createIdentity();
        var before = jdbc.queryForMap("SELECT id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        UUID executionId = insertExecutionSnapshot(ref);
        var executionBefore = jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId);
        verify.verify(command(ref, 0));
        var after = jdbc.queryForMap("SELECT id, namespace, external_id, jogador_id, identity_class, ownership_status, verification_status, ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", ref.namespace(), ref.externalId());
        assertThat(after).containsEntry("id", before.get("id")).containsEntry("namespace", ref.namespace())
                .containsEntry("external_id", ref.externalId()).containsEntry("jogador_id", before.get("jogador_id"))
                .containsEntry("identity_class", "EXTERNAL").containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "VERIFIED").containsEntry("ownership_version", 1L);
        var history = jdbc.queryForMap("SELECT aggregate_version,event_type,previous_identity_class,new_identity_class,previous_target_jogador_id,new_target_jogador_id,previous_ownership_status,new_ownership_status,previous_verification_status,new_verification_status,provenance,actor_type,actor_id,evidence_type,evidence_reference,reason,effective_at,recorded_at FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFIED'", ref.namespace(), ref.externalId());
        assertThat(history).containsEntry("aggregate_version", 1L).containsEntry("event_type", "OWNERSHIP_VERIFIED")
                .containsEntry("previous_identity_class", "EXTERNAL").containsEntry("new_identity_class", "EXTERNAL")
                .containsEntry("previous_target_jogador_id", before.get("jogador_id"))
                .containsEntry("new_target_jogador_id", before.get("jogador_id"))
                .containsEntry("previous_ownership_status", "ACTIVE").containsEntry("new_ownership_status", "ACTIVE")
                .containsEntry("previous_verification_status", "UNVERIFIED").containsEntry("new_verification_status", "VERIFIED")
                .containsEntry("provenance", "LOGOS_OPERATOR_ACTION").containsEntry("actor_type", "WORKLOAD_OPERATOR")
                .containsEntry("actor_id", "verified-workload-1").containsEntry("evidence_type", "operator-review")
                .containsEntry("reason", "evidence reviewed");
        assertThat(history.get("evidence_reference").toString()).startsWith("case-");
        assertThat(history.get("effective_at")).isNotNull();
        assertThat(history.get("recorded_at")).isNotNull();
        assertThat(jdbc.queryForMap("SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at FROM progression_external_execution WHERE id = ?", executionId))
                .isEqualTo(executionBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=?", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(2);
    }

    private UUID insertExecutionSnapshot(ExternalSubjectReference ref) {
        UUID configurationId = UUID.randomUUID();
        UUID configurationVersionId = UUID.randomUUID();
        UUID skillPolicyId = UUID.randomUUID();
        UUID skillPolicyVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition (id, logical_key) VALUES (?, ?)", configurationId, "verify-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_configuration_version (id, definition_id, revision, base_xp, base_stress) VALUES (?, ?, 1, 1, 0)", configurationVersionId, configurationId);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", configurationVersionId, configurationId);
        jdbc.update("INSERT INTO progression_skill_policy (id, logical_key) VALUES (?, ?)", skillPolicyId, "verify-" + UUID.randomUUID());
        jdbc.update("INSERT INTO progression_skill_policy_version (id, policy_id, revision) VALUES (?, ?, 1)", skillPolicyVersionId, skillPolicyId);
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?", skillPolicyVersionId, skillPolicyId);
        UUID executionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO progression_external_execution
                    (id, source_system, idempotency_key, request_fingerprint, response_json, request_json,
                     processing_status, attempt_count, last_error, subject_namespace, subject_external_id,
                     configuration_key, requested_revision, configuration_version_id,
                     skill_policy_version_id, created_at)
                VALUES (?, 'verify-snapshot', ?, 'fingerprint', '{\"result\":\"stable\"}', ?,
                        'COMPLETED', 1, NULL, ?, ?, 'reading', 1, ?, ?, CURRENT_TIMESTAMP)
                """, executionId, "idempotency-" + UUID.randomUUID(),
                "{\"subject\":{\"namespace\":\"" + ref.namespace() + "\",\"externalId\":\"" + ref.externalId() + "\"}}",
                ref.namespace(), ref.externalId(), configurationVersionId, skillPolicyVersionId);
        return executionId;
    }

    @Test void verifiedReplayIsNoOpEvenWhenVersionIsStale() {
        var ref = createIdentity();
        verify.verify(command(ref, 0));
        verify.verify(new VerifyExternalSubjectOwnershipCommand(ref, 0, null, null, null));
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=?", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(2);
    }

    @Test void historyFailureRollsBackTheCurrentRow() {
        var ref = createIdentity();
        jdbc.execute("CREATE FUNCTION reject_verify_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type = 'OWNERSHIP_VERIFIED' THEN RAISE EXCEPTION 'intentional verify history failure'; END IF; RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER reject_verify_history BEFORE INSERT ON progression_subject_ownership_history FOR EACH ROW EXECUTE FUNCTION reject_verify_history()");
        try {
            assertThatThrownBy(() -> verify.verify(command(ref, 0))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER reject_verify_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_verify_history()");
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("UNVERIFIED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=?", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
    }

    @Test void concurrentSameVersionVerificationsProduceOneMutationAndHistoryEvent() throws Exception {
        var ref = createIdentity();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> { start.await(); verify.verify(command(ref, 0)); return null; });
            var second = pool.submit(() -> { start.await(); verify.verify(command(ref, 0)); return null; });
            start.countDown();
            first.get(); second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace=? AND external_id=?", String.class, ref.namespace(), ref.externalId())).isEqualTo("VERIFIED");
        assertThat(jdbc.queryForObject("SELECT ownership_version FROM progression_subject_identity WHERE namespace=? AND external_id=?", Long.class, ref.namespace(), ref.externalId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_ownership_history h JOIN progression_subject_identity i ON i.id=h.identity_id WHERE i.namespace=? AND i.external_id=? AND h.event_type='OWNERSHIP_VERIFIED'", Integer.class, ref.namespace(), ref.externalId())).isEqualTo(1);
    }
}
