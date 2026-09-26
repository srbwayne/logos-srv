package com.josecjuniors.logossrv.support.test;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class WorkloadTrustRegistryMigrationPostgresTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;

    @BeforeEach
    void cleanTrustTables() {
        jdbc.update("DELETE FROM workload_assertion_replay");
        jdbc.update("DELETE FROM workload_trust_audit_event");
        jdbc.update("DELETE FROM workload_signing_key");
        jdbc.update("DELETE FROM workload_principal");
    }

    @AfterEach
    void leaveNoTrustFixtures() {
        jdbc.update("DELETE FROM workload_assertion_replay");
        jdbc.update("DELETE FROM workload_trust_audit_event");
        jdbc.update("DELETE FROM workload_signing_key");
        jdbc.update("DELETE FROM workload_principal");
    }

    @Test
    void v1ToV45CreatesAllTrustTablesWithoutSeedingTrust() {
        assertThat(jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("45");

        for (String table : new String[]{"workload_principal", "workload_signing_key",
                "workload_trust_audit_event", "workload_assertion_replay"}) {
            assertThat(tableExists(table)).as("table %s", table).isTrue();
            assertThat(count(table)).as("empty table %s", table).isZero();
        }
        assertThat(jdbc.queryForObject(
                "SELECT col_description('workload_principal'::regclass, "
                        + "(SELECT ordinal_position FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND table_name = 'workload_principal' "
                        + "AND column_name = 'issuer'))",
                String.class)).contains("immutable");
    }

    @Test
    void principalIdentityAndIssuerAreUnique() {
        insertPrincipal("principal-one", "issuer-one");
        assertThatThrownBy(() -> insertPrincipal("principal-one", "issuer-two"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPrincipal("principal-two", "issuer-one"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void principalRejectsBlankIdentityAndNonWorkloadType() {
        assertThatThrownBy(() -> insertPrincipal("   ", "issuer-blank-subject"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPrincipal("valid-id", "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO workload_principal (principal_type, principal_id, issuer, lifecycle_status)
                VALUES ('APP_USER', 'some-user', 'issuer-app-user', 'ACTIVE')
                """ )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void principalLifecycleAllowsDisableThenRevokeAndPreservesDisableTime() {
        insertPrincipal("active", "issuer-active");
        assertThatThrownBy(() -> insertPrincipal("active-with-disabled-time", "issuer-active-disabled",
                "ACTIVE", timestamp(), null)).isInstanceOf(DataIntegrityViolationException.class);
        insertPrincipal("disabled", "issuer-disabled", "DISABLED", timestamp(), null);
        assertThatThrownBy(() -> insertPrincipal("disabled-no-time", "issuer-disabled-no-time",
                "DISABLED", null, null)).isInstanceOf(DataIntegrityViolationException.class);
        insertPrincipal("revoked", "issuer-revoked", "REVOKED", null, timestamp());
        insertPrincipal("disabled-then-revoked", "issuer-disabled-revoked", "REVOKED", timestamp(), timestamp());
        assertThatThrownBy(() -> insertPrincipal("revoked-no-time", "issuer-revoked-no-time",
                "REVOKED", null, null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void keyKidAlgorithmAndWorkloadScopedUniquenessAreEnforced() {
        UUID firstPrincipal = insertPrincipal("key-owner-one", "issuer-key-one");
        UUID secondPrincipal = insertPrincipal("key-owner-two", "issuer-key-two");
        insertKey(firstPrincipal, "kid-A", "ES256", new byte[32], "ACTIVE", timestamp(), null, null, null);

        assertThatThrownBy(() -> insertKey(firstPrincipal, "kid-A", "ES256", filled(1),
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertKey(secondPrincipal, "kid-A", "ES256", filled(2), "ACTIVE", timestamp(), null, null, null);

        assertThatThrownBy(() -> insertKey(firstPrincipal, "../bad", "ES256", filled(3),
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertKey(firstPrincipal, "kid-other", "EdDSA", filled(4),
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 33})
    void keyFingerprintMustBeExactly32Bytes(int length) {
        UUID principal = insertPrincipal("fingerprint-length-" + length, "issuer-fp-length-" + length);
        assertThatThrownBy(() -> insertKey(principal, "kid-fp", "ES256", new byte[length],
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publicKeyFingerprintIsGloballyUniqueAcrossKidsAndPrincipals() {
        UUID first = insertPrincipal("fingerprint-owner-one", "issuer-fingerprint-one");
        UUID second = insertPrincipal("fingerprint-owner-two", "issuer-fingerprint-two");
        byte[] fingerprint = filled(11);
        insertKey(first, "kid-one", "ES256", fingerprint, "ACTIVE", timestamp(), null, null, null);

        assertThatThrownBy(() -> insertKey(first, "kid-two", "ES256", fingerprint,
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertKey(second, "kid-one", "ES256", fingerprint,
                "ACTIVE", timestamp(), null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertKey(second, "kid-two", "ES256", filled(12), "ACTIVE", timestamp(), null, null, null);
    }

    @Test
    void signingKeyLifecycleRejectsImpossibleTimestampCombinations() {
        UUID principal = insertPrincipal("key-lifecycle", "issuer-key-lifecycle");
        insertKey(principal, "pending", "ES256", filled(20), "PENDING", null, null, null, null);
        insertKey(principal, "active", "ES256", filled(21), "ACTIVE", timestamp(), null, null, null);
        insertKey(principal, "revoked-pending", "ES256", filled(22), "REVOKED", null, timestamp(), null, null);
        insertKey(principal, "retired", "ES256", filled(23), "RETIRED", timestamp(), null, timestamp(), null);
        insertKey(principal, "revoked-retired", "ES256", filled(24), "RETIRED", timestamp(),
                timestamp(), timestamp(), null);

        assertThatThrownBy(() -> insertKey(principal, "active-no-activation", "ES256", filled(25),
                "ACTIVE", null, null, null, null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertKey(principal, "pending-activation", "ES256", filled(26),
                "PENDING", timestamp(), null, null, null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertKey(principal, "retired-no-activation", "ES256", filled(27),
                "RETIRED", null, null, timestamp(), null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertKey(principal, "bad-window", "ES256", filled(28),
                "PENDING", null, null, null, timestamp())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publicKeyPemMustNotBeBlank() {
        UUID principal = insertPrincipal("pem-blank", "issuer-pem-blank");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO workload_signing_key
                    (workload_principal_id, kid, algorithm, public_key_pem,
                     public_key_spki_sha256, lifecycle_status, not_before, activated_at)
                VALUES (?, 'kid-pem', 'ES256', '   ', ?, 'ACTIVE', clock_timestamp(), clock_timestamp())
                """, principal, filled(29)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void auditRequiresValidReferencesNonblankFieldsAndObjectMetadata() {
        UUID principal = insertPrincipal("audit-owner", "issuer-audit");
        UUID credential = insertKey(principal, "kid-audit", "ES256", filled(30), "ACTIVE",
                timestamp(), null, null, null);
        insertAudit(principal, credential, "OPERATOR", "ops-change-123", "KEY_ACTIVATED", "reviewed",
                "{\"source\":\"operator\"}");

        assertThatThrownBy(() -> insertAudit(principal, credential, "OPERATOR", "ops-change-124",
                "KEY_UPDATED", "invalid array metadata", "[]"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(principal, credential, "USER", "some-user",
                "KEY_UPDATED", "invalid actor", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(principal, credential, "OPERATOR", "   ",
                "KEY_UPDATED", "invalid actor id", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(principal, credential, "SYSTEM", "logos",
                "   ", "invalid action", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(principal, credential, "SYSTEM", "logos",
                "KEY_UPDATED", "   ", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(UUID.randomUUID(), credential, "SYSTEM", "logos",
                "KEY_UPDATED", "unknown principal", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAudit(principal, UUID.randomUUID(), "SYSTEM", "logos",
                "KEY_UPDATED", "unknown credential", "{}"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void replayPrimaryKeyIsIssuerAndUuidJtiAndHasNoProgressionForeignKeys() {
        UUID principal = insertPrincipal("replay-owner", "issuer-replay");
        UUID jti = UUID.randomUUID();
        insertReplay("issuer-replay", jti);
        assertThatThrownBy(() -> insertReplay("issuer-replay", jti))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertReplay("issuer-replay", UUID.randomUUID());
        assertThatThrownBy(() -> insertReplay("issuer-unknown", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(principal).isNotNull();

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.key_column_usage
                WHERE table_schema = current_schema()
                  AND table_name = 'workload_assertion_replay'
                  AND constraint_name = 'pk_workload_assertion_replay'
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.key_column_usage k
                JOIN information_schema.table_constraints c
                  ON c.constraint_schema = k.constraint_schema
                 AND c.constraint_name = k.constraint_name
                WHERE k.table_schema = current_schema()
                  AND k.table_name = 'workload_assertion_replay'
                  AND c.constraint_type = 'FOREIGN KEY'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename = 'workload_assertion_replay'
                  AND indexname = 'ix_workload_assertion_replay_expires_at'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void referencedPrincipalsAndCredentialsCannotBeDeleted() {
        UUID principal = insertPrincipal("delete-protected", "issuer-delete-protected");
        UUID credential = insertKey(principal, "kid-delete-protected", "ES256", filled(40),
                "ACTIVE", timestamp(), null, null, null);
        insertAudit(principal, credential, "SYSTEM", "migration-test", "KEY_CREATED", "fixture", "{}");
        insertReplay("issuer-delete-protected", UUID.randomUUID());

        assertThatThrownBy(() -> jdbc.update("DELETE FROM workload_principal WHERE id = ?", principal))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM workload_signing_key WHERE id = ?", credential))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v44DataRemainsUnchangedAfterApplyingOnlyV45() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_v45_preserve_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;

        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }

        try {
            Flyway.configure().dataSource(baseUrl, username, password)
                    .locations("classpath:db/migration").schemas(schema).defaultSchema(schema)
                    .createSchemas(false).target(MigrationVersion.fromVersion("44")).load().migrate();

            UUID userId = UUID.randomUUID();
            UUID jogadorId = UUID.randomUUID();
            UUID identityId = UUID.randomUUID();
            UUID attributeId = UUID.randomUUID();
            UUID definitionId = UUID.randomUUID();
            UUID configurationVersionId = UUID.randomUUID();
            UUID skillPolicyId = UUID.randomUUID();
            UUID skillPolicyVersionId = UUID.randomUUID();
            UUID executionId = UUID.randomUUID();

            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO app_user (id, email, password) VALUES ('" + userId
                        + "', 'v44-preserve@example.test', 'fixture-hash')");
                statement.executeUpdate("INSERT INTO jogador (id, user_id, apelido) VALUES ('" + jogadorId
                        + "', '" + userId + "', 'v44-preserve-player')");
                statement.executeUpdate("INSERT INTO progression_subject_identity (id, namespace, external_id, jogador_id) "
                        + "VALUES ('" + identityId + "', 'logos-native', 'v44-preserve', '" + jogadorId + "')");
                statement.executeUpdate("INSERT INTO atributo (id, nome, semantic_key) VALUES ('" + attributeId
                        + "', 'V44 Preserve Attribute', 'v44-preserve-semantic-key')");
                statement.executeUpdate("INSERT INTO progression_configuration_definition (id, logical_key) VALUES ('"
                        + definitionId + "', 'v44-preserve-configuration')");
                statement.executeUpdate("INSERT INTO progression_configuration_version "
                        + "(id, definition_id, revision, base_xp, base_stress) VALUES ('" + configurationVersionId
                        + "', '" + definitionId + "', 1, 7, 2)");
                statement.executeUpdate("UPDATE progression_configuration_definition SET current_version_id = '"
                        + configurationVersionId + "' WHERE id = '" + definitionId + "'");
                statement.executeUpdate("INSERT INTO progression_skill_policy (id, logical_key) VALUES ('"
                        + skillPolicyId + "', 'v44-preserve-policy')");
                statement.executeUpdate("INSERT INTO progression_skill_policy_version (id, policy_id, revision) VALUES ('"
                        + skillPolicyVersionId + "', '" + skillPolicyId + "', 1)");
                statement.executeUpdate("UPDATE progression_skill_policy SET current_version_id = '"
                        + skillPolicyVersionId + "' WHERE id = '" + skillPolicyId + "'");
                statement.executeUpdate("INSERT INTO progression_external_execution "
                        + "(id, source_system, idempotency_key, request_fingerprint, subject_namespace, "
                        + "subject_external_id, configuration_key, requested_revision, configuration_version_id, "
                        + "skill_policy_version_id, response_json, created_at, request_json, processing_status, attempt_count) "
                        + "VALUES ('" + executionId + "', 'v44-source', 'v44-idempotency', 'fingerprint', "
                        + "'logos-native', 'v44-preserve', 'v44-preserve-configuration', 1, '"
                        + configurationVersionId + "', '" + skillPolicyVersionId + "', '{\"outcome\":7}', "
                        + "TIMESTAMP '2026-01-02 03:04:05', '{\"request\":true}', 'COMPLETED', 1)");
            }

            String beforeUser = rowJson(isolatedUrl, username, password, "app_user", userId);
            String beforePlayer = rowJson(isolatedUrl, username, password, "jogador", jogadorId);
            String beforeIdentity = rowJson(isolatedUrl, username, password, "progression_subject_identity", identityId);
            String beforeExecution = rowJson(isolatedUrl, username, password, "progression_external_execution", executionId);
            String beforeAttribute = rowJson(isolatedUrl, username, password, "atributo", attributeId);

            Flyway.configure().dataSource(baseUrl, username, password)
                    .locations("classpath:db/migration").schemas(schema).defaultSchema(schema)
                    .createSchemas(false).load().migrate();

            assertThat(rowJson(isolatedUrl, username, password, "app_user", userId)).isEqualTo(beforeUser);
            assertThat(rowJson(isolatedUrl, username, password, "jogador", jogadorId)).isEqualTo(beforePlayer);
            assertThat(rowJson(isolatedUrl, username, password, "progression_subject_identity", identityId))
                    .isEqualTo(beforeIdentity);
            assertThat(rowJson(isolatedUrl, username, password, "progression_external_execution", executionId))
                    .isEqualTo(beforeExecution);
            assertThat(rowJson(isolatedUrl, username, password, "atributo", attributeId)).isEqualTo(beforeAttribute);
            assertThat(rowCount(isolatedUrl, username, password, "workload_principal")).isZero();
            assertThat(rowCount(isolatedUrl, username, password, "workload_signing_key")).isZero();
            assertThat(rowCount(isolatedUrl, username, password, "workload_trust_audit_event")).isZero();
            assertThat(rowCount(isolatedUrl, username, password, "workload_assertion_replay")).isZero();
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    private UUID insertPrincipal(String principalId, String issuer) {
        return insertPrincipal(principalId, issuer, "ACTIVE", null, null);
    }

    private UUID insertPrincipal(String principalId, String issuer, String status,
                                 Timestamp disabledAt, Timestamp revokedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO workload_principal
                    (id, principal_type, principal_id, issuer, lifecycle_status, disabled_at, revoked_at)
                VALUES (?, 'WORKLOAD', ?, ?, ?, ?, ?)
                """, id, principalId, issuer, status, disabledAt, revokedAt);
        return id;
    }

    private UUID insertKey(UUID principalId, String kid, String algorithm, byte[] fingerprint,
                           String status, Timestamp activatedAt, Timestamp revokedAt,
                           Timestamp retiredAt, Timestamp notAfter) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (id, workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, not_after, activated_at, revoked_at, retired_at)
                VALUES (?, ?, ?, ?, 'synthetic public PEM fixture', ?, ?, clock_timestamp(), ?, ?, ?, ?)
                """, id, principalId, kid, algorithm, fingerprint, status, notAfter,
                activatedAt, revokedAt, retiredAt);
        return id;
    }

    private void insertAudit(UUID principalId, UUID credentialId, String actorType, String actorId,
                             String action, String reason, String metadataJson) {
        jdbc.update("""
                INSERT INTO workload_trust_audit_event
                    (workload_principal_id, credential_id, actor_type, actor_id, action, reason, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                """, principalId, credentialId, actorType, actorId, action, reason, metadataJson);
    }

    private void insertReplay(String issuer, UUID jti) {
        jdbc.update("""
                INSERT INTO workload_assertion_replay (issuer, jti, expires_at)
                VALUES (?, ?, clock_timestamp() + INTERVAL '60 seconds')
                """, issuer, jti);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private boolean tableExists(String table) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name = ?)
                """, Boolean.class, table);
    }

    private static Timestamp timestamp() {
        return Timestamp.from(Instant.parse("2026-01-02T03:04:05Z"));
    }

    private static byte[] filled(int value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    private static String withoutCurrentSchema(String url) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) return url;
        String base = url.substring(0, queryStart);
        String params = url.substring(queryStart + 1).replaceAll("(^|&)currentSchema=[^&]*&?", "")
                .replaceAll("&&", "&").replaceAll("(^&|&$)", "");
        return params.isBlank() ? base : base + "?" + params;
    }

    private static String rowJson(String url, String username, String password, String table, UUID id)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             var statement = connection.prepareStatement("SELECT to_jsonb(t)::text FROM " + table + " t WHERE id = ?")) {
            statement.setObject(1, id);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).as("row %s/%s exists", table, id).isTrue();
                return result.getString(1);
            }
        }
    }

    private static long rowCount(String url, String username, String password, String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }
}
