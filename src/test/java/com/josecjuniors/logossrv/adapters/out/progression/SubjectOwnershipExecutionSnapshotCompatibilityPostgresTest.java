package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProvisionCurrentExternalSubjectIdentityService;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-execution-snapshot-c1a")
class SubjectOwnershipExecutionSnapshotCompatibilityPostgresTest {

    @Autowired RegistrationUseCase registration;
    @Autowired ProvisionCurrentExternalSubjectIdentityService provisioning;
    @Autowired JdbcTemplate jdbc;

    @Test
    void idempotentPocProvisioningKeepsDurableExecutionReferenceUnchanged() {
        String email = "c1a-snapshot-" + UUID.randomUUID() + "@example.test";
        var registered = registration.register(new RegistrationCommand(email, "password", "Snapshot POC"));
        UUID appUserId = registered.user().getId().getValue();
        var reference = new ExternalSubjectReference("lifeos", "snapshot-" + UUID.randomUUID());
        provisioning.provision(email, reference);

        UUID configurationVersionId = jdbc.queryForObject("""
                SELECT current_version_id FROM progression_configuration_definition WHERE logical_key = 'reading'
                """, UUID.class);
        UUID skillPolicyVersionId = jdbc.queryForObject("""
                SELECT current_version_id FROM progression_skill_policy WHERE logical_key = 'global'
                """, UUID.class);
        UUID executionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO progression_external_execution
                    (id, source_system, idempotency_key, request_fingerprint, response_json, request_json,
                     processing_status, attempt_count, last_error, subject_namespace, subject_external_id,
                     configuration_key, requested_revision, configuration_version_id,
                     skill_policy_version_id, created_at)
                VALUES (?, 'c1a-snapshot', ?, 'fingerprint', '{"result":"stable"}', ?,
                        'COMPLETED', 1, NULL, ?, ?, 'reading', 1, ?, ?, CURRENT_TIMESTAMP)
                """, executionId, "idempotency-" + UUID.randomUUID(),
                "{\"subject\":{\"namespace\":\"" + reference.namespace() + "\",\"externalId\":\""
                        + reference.externalId() + "\"}}",
                reference.namespace(), reference.externalId(), configurationVersionId, skillPolicyVersionId);

        Map<String, Object> before = executionSnapshot(executionId);
        provisioning.provision(email, reference);
        Map<String, Object> after = executionSnapshot(executionId);

        assertThat(after).isEqualTo(before);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT actor_id FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, String.class, reference.namespace(), reference.externalId())).isEqualTo(appUserId.toString());
        assertThat(jdbc.queryForObject("""
                SELECT identity.verification_status FROM progression_subject_identity identity
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, String.class, reference.namespace(), reference.externalId())).isEqualTo("UNVERIFIED");
    }

    private Map<String, Object> executionSnapshot(UUID executionId) {
        return jdbc.queryForMap("""
                SELECT subject_namespace, subject_external_id, request_json, response_json, occurred_at
                FROM progression_external_execution WHERE id = ?
                """, executionId);
    }
}
