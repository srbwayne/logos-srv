package com.josecjuniors.logossrv.support.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class AuthorizationGrantRegistryMigrationPostgresTest {
    @Autowired JdbcTemplate jdbc;
    private final UUID principalId = UUID.randomUUID();
    private final UUID principalDbId = UUID.randomUUID();
    private final String issuer = "urn:logos:test:authorization-migration:" + principalId;

    @BeforeEach
    void createPrincipal() {
        jdbc.update("""
                INSERT INTO workload_principal (id, principal_type, principal_id, issuer, lifecycle_status)
                VALUES (?, 'WORKLOAD', ?, ?, 'ACTIVE')
                """, principalDbId, principalId.toString(), issuer);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM authorization_grant WHERE principal_id = ?", principalId.toString());
        jdbc.update("DELETE FROM workload_principal WHERE id = ?", principalDbId);
    }

    @Test
    void migrationCreatesEmptyRegistryAndEnforcesAllStructuralConstraints() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_principal", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_signing_key", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_name='authorization_grant' AND column_name='updated_at'", Long.class)).isZero();

        assertInvalid("WORKLOAD", " ", "PROGRESSION_EXECUTE", "lifeos", "lifeos");
        assertInvalid("HUMAN", principalId.toString(), "PROGRESSION_EXECUTE", "lifeos", "lifeos");
        assertInvalid("WORKLOAD", " " + principalId, "PROGRESSION_EXECUTE", "lifeos", "lifeos");
        assertInvalid("WORKLOAD", principalId.toString(), "UNKNOWN", "lifeos", "lifeos");
        assertInvalid("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "LifeOS", "lifeos");
        assertInvalid("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "lifeos", "LifeOS");
        assertInvalid("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "*", "lifeos");
        assertInvalid("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "lifeos", "*");
        assertInvalid("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "lifeos", null);
        assertInvalid("WORKLOAD", UUID.randomUUID().toString(), "PROGRESSION_EXECUTE", "lifeos", "lifeos");

        insert("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTION_READ", "lifeos", null);
        assertThatThrownBy(() -> insert("WORKLOAD", principalId.toString(),
                "PROGRESSION_EXECUTION_READ", "lifeos", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class)).isEqualTo(1L);
    }

    @Test
    void ownershipManagementGrantRequiresOnlyAnExactNamespace() {
        assertInvalid("WORKLOAD", principalId.toString(), "SUBJECT_OWNERSHIP_MANAGE", "lifeos", "lifeos");
        assertInvalid("WORKLOAD", principalId.toString(), "SUBJECT_OWNERSHIP_MANAGE", null, null);

        insert("WORKLOAD", principalId.toString(), "SUBJECT_OWNERSHIP_MANAGE", null, "lifeos");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant WHERE operation = ?",
                Long.class, "SUBJECT_OWNERSHIP_MANAGE")).isEqualTo(1L);
    }

    @Test
    void migrationPreservesApplicabilityOfEveryExistingOperation() {
        insert("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTE", "lifeos", "lifeos");
        insert("WORKLOAD", principalId.toString(), "PROGRESSION_EXECUTION_READ", "lifeos", null);
        insert("WORKLOAD", principalId.toString(), "PROGRESSION_HISTORY_READ", null, "lifeos");
        insert("WORKLOAD", principalId.toString(), "SUBJECT_PROVISION", null, "lifeos");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM authorization_grant", Long.class)).isEqualTo(4L);
    }

    private void assertInvalid(String type, String id, String operation, String source, String namespace) {
        assertThatThrownBy(() -> insert(type, id, operation, source, namespace))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(String type, String id, String operation, String source, String namespace) {
        jdbc.update("""
                INSERT INTO authorization_grant (principal_type, principal_id, operation, source, namespace)
                VALUES (?, ?, ?, ?, ?)
                """, type, id, operation, source, namespace);
    }
}
