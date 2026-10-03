package com.josecjuniors.logossrv.support.test;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class SubjectOwnershipFoundationMigrationPostgresTest {

    @Autowired Environment environment;

    @Test
    void v47TruthfullyClassifiesExistingRowsAndPreservesExecutionSnapshots() throws Exception {
        String baseUrl = withoutCurrentSchema(environment.getRequiredProperty("spring.datasource.url"));
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "logos_c1a_v47_" + UUID.randomUUID().toString().replace("-", "");
        String isolatedUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;

        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }

        try {
            flyway(baseUrl, username, password, schema, "46").migrate();
            UUID nativeUser = UUID.randomUUID();
            UUID externalUser = UUID.randomUUID();
            UUID nativePlayer = UUID.randomUUID();
            UUID externalPlayer = UUID.randomUUID();
            UUID nativeIdentity = UUID.randomUUID();
            UUID externalIdentity = UUID.randomUUID();
            UUID configurationId = UUID.randomUUID();
            UUID configurationVersionId = UUID.randomUUID();
            UUID skillPolicyId = UUID.randomUUID();
            UUID skillPolicyVersionId = UUID.randomUUID();
            UUID executionId = UUID.randomUUID();

            try (Connection connection = DriverManager.getConnection(isolatedUrl, username, password)) {
                insert(connection, "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                        nativeUser, "native-legacy@example.test", "fixture-hash");
                insert(connection, "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                        externalUser, "external-legacy@example.test", "fixture-hash");
                insert(connection, "INSERT INTO jogador (id, user_id, apelido) VALUES (?, ?, ?)",
                        nativePlayer, nativeUser, "native-legacy");
                insert(connection, "INSERT INTO jogador (id, user_id, apelido) VALUES (?, ?, ?)",
                        externalPlayer, externalUser, "external-legacy");
                insert(connection, """
                        INSERT INTO progression_subject_identity (id, namespace, external_id, jogador_id)
                        VALUES (?, 'logos-native', ?, ?)
                        """, nativeIdentity, nativeUser.toString(), nativePlayer);
                insert(connection, """
                        INSERT INTO progression_subject_identity (id, namespace, external_id, jogador_id)
                        VALUES (?, 'lifeos', 'legacy-external-id', ?)
                        """, externalIdentity, externalPlayer);

                insert(connection, "INSERT INTO progression_configuration_definition (id, logical_key) VALUES (?, ?)",
                        configurationId, "c1a-migration-config");
                insert(connection, """
                        INSERT INTO progression_configuration_version
                            (id, definition_id, revision, base_xp, base_stress, fact_key_generation)
                        VALUES (?, ?, 1, 1, 0, 'SEMANTIC')
                        """, configurationVersionId, configurationId);
                insert(connection, "UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?",
                        configurationVersionId, configurationId);
                insert(connection, "INSERT INTO progression_skill_policy (id, logical_key) VALUES (?, ?)",
                        skillPolicyId, "c1a-migration-policy");
                insert(connection, "INSERT INTO progression_skill_policy_version (id, policy_id, revision) VALUES (?, ?, 1)",
                        skillPolicyVersionId, skillPolicyId);
                insert(connection, "UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?",
                        skillPolicyVersionId, skillPolicyId);
                insert(connection, """
                        INSERT INTO progression_external_execution
                            (id, source_system, idempotency_key, request_fingerprint, subject_namespace,
                             subject_external_id, configuration_key, requested_revision,
                             configuration_version_id, skill_policy_version_id, response_json,
                             created_at, request_json, processing_status, attempt_count)
                        VALUES (?, 'migration-test', 'snapshot-key', 'fingerprint', 'lifeos',
                                'legacy-external-id', 'c1a-migration-config', 1, ?, ?,
                                '{"result":"preserve"}', TIMESTAMP '2026-01-02 03:04:05',
                                '{"subject":"lifeos:legacy-external-id"}', 'COMPLETED', 1)
                        """, executionId, configurationVersionId, skillPolicyVersionId);
            }

            Map<String, Object> nativeBefore = identityBefore(isolatedUrl, username, password, nativeIdentity);
            Map<String, Object> externalBefore = identityBefore(isolatedUrl, username, password, externalIdentity);
            Map<String, Object> executionBefore = execution(isolatedUrl, username, password, executionId);

            flyway(baseUrl, username, password, schema, "47").migrate();

            assertThat(latestVersion(isolatedUrl, username, password)).isEqualTo("47");
            assertThat(identity(isolatedUrl, username, password, nativeIdentity)).containsAllEntriesOf(nativeBefore)
                    .containsEntry("identity_class", "LOGOS_NATIVE")
                    .containsEntry("ownership_status", "ACTIVE")
                    .containsEntry("verification_status", "NOT_REQUIRED")
                    .containsEntry("ownership_version", 0L);
            assertThat(identity(isolatedUrl, username, password, externalIdentity)).containsAllEntriesOf(externalBefore)
                    .containsEntry("identity_class", "EXTERNAL")
                    .containsEntry("ownership_status", "ACTIVE")
                    .containsEntry("verification_status", "UNVERIFIED")
                    .containsEntry("ownership_version", 0L);
            assertThat(execution(isolatedUrl, username, password, executionId)).isEqualTo(executionBefore);

            assertHistory(isolatedUrl, username, password, nativeIdentity, nativePlayer,
                    "LOGOS_NATIVE", "NOT_REQUIRED", "LEGACY_LOGOS_NATIVE_UNKNOWN");
            assertHistory(isolatedUrl, username, password, externalIdentity, externalPlayer,
                    "EXTERNAL", "UNVERIFIED", "LEGACY_EXTERNAL_UNKNOWN");
            assertThat(queryLong(isolatedUrl, username, password,
                    "SELECT count(*) FROM progression_subject_ownership_history")).isEqualTo(2L);

            assertThatThrownBy(() -> execute(isolatedUrl, username, password,
                    "UPDATE progression_subject_ownership_history SET reason = 'changed' WHERE identity_id = '"
                            + nativeIdentity + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("append-only");
            assertThatThrownBy(() -> execute(isolatedUrl, username, password,
                    "DELETE FROM progression_subject_ownership_history WHERE identity_id = '" + nativeIdentity + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("append-only");
            assertThatThrownBy(() -> execute(isolatedUrl, username, password,
                    "DELETE FROM progression_subject_identity WHERE id = '" + nativeIdentity + "'"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(isolatedUrl, username, password,
                    "UPDATE progression_subject_identity SET identity_class = 'EXTERNAL' WHERE id = '"
                            + nativeIdentity + "'"))
                    .isInstanceOf(SQLException.class);
        } finally {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    private static Flyway flyway(String url, String username, String password, String schema, String target) {
        return Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").schemas(schema).defaultSchema(schema)
                .createSchemas(false).target(MigrationVersion.fromVersion(target)).load();
    }

    private static void insert(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
        }
    }

    private static Map<String, Object> identity(String url, String username, String password, UUID id)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, namespace, external_id, jogador_id, identity_class, ownership_status,
                            verification_status, ownership_version
                     FROM progression_subject_identity WHERE id = ?
                     """)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return Map.of("id", result.getObject("id"),
                        "namespace", result.getString("namespace"),
                        "external_id", result.getString("external_id"),
                        "jogador_id", result.getObject("jogador_id"),
                        "identity_class", result.getString("identity_class"),
                        "ownership_status", result.getString("ownership_status"),
                        "verification_status", result.getString("verification_status"),
                        "ownership_version", result.getLong("ownership_version"));
            }
        }
    }

    private static Map<String, Object> identityBefore(String url, String username, String password, UUID id)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, namespace, external_id, jogador_id
                     FROM progression_subject_identity WHERE id = ?
                     """)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return Map.of("id", result.getObject("id"),
                        "namespace", result.getString("namespace"),
                        "external_id", result.getString("external_id"),
                        "jogador_id", result.getObject("jogador_id"));
            }
        }
    }

    private static Map<String, Object> execution(String url, String username, String password, UUID id)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT subject_namespace, subject_external_id, request_json, response_json,
                            occurred_at::text AS occurred_at
                     FROM progression_external_execution WHERE id = ?
                     """)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return Map.of("subject_namespace", result.getString("subject_namespace"),
                        "subject_external_id", result.getString("subject_external_id"),
                        "request_json", result.getString("request_json"),
                        "response_json", result.getString("response_json"),
                        "occurred_at", result.getString("occurred_at"));
            }
        }
    }

    private static void assertHistory(String url, String username, String password, UUID identityId,
                                      UUID targetId, String identityClass, String verificationStatus,
                                      String provenance) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT aggregate_version, event_type, previous_identity_class, new_identity_class,
                            previous_target_jogador_id, new_target_jogador_id,
                            previous_ownership_status, new_ownership_status,
                            previous_verification_status, new_verification_status, provenance,
                            actor_type, actor_id, evidence_type, evidence_reference, reason,
                            effective_at, recorded_at
                     FROM progression_subject_ownership_history WHERE identity_id = ?
                     """)) {
            statement.setObject(1, identityId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("aggregate_version")).isZero();
                assertThat(result.getString("event_type")).isEqualTo("INITIAL_CLASSIFICATION");
                assertThat(result.getString("previous_identity_class")).isNull();
                assertThat(result.getString("new_identity_class")).isEqualTo(identityClass);
                assertThat(result.getObject("previous_target_jogador_id")).isNull();
                assertThat(result.getObject("new_target_jogador_id")).isEqualTo(targetId);
                assertThat(result.getString("previous_ownership_status")).isNull();
                assertThat(result.getString("new_ownership_status")).isEqualTo("ACTIVE");
                assertThat(result.getString("previous_verification_status")).isNull();
                assertThat(result.getString("new_verification_status")).isEqualTo(verificationStatus);
                assertThat(result.getString("provenance")).isEqualTo(provenance);
                assertThat(result.getString("actor_type")).isNull();
                assertThat(result.getString("actor_id")).isNull();
                assertThat(result.getString("evidence_type")).isNull();
                assertThat(result.getString("evidence_reference")).isNull();
                assertThat(result.getString("reason")).isNull();
                assertThat(result.getObject("effective_at")).isNull();
                assertThat(result.getObject("recorded_at")).isNotNull();
                assertThat(result.next()).isFalse();
            }
        }
    }

    private static long queryLong(String url, String username, String password, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static void execute(String url, String username, String password, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static String latestVersion(String url, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT version FROM flyway_schema_history WHERE success = true
                     ORDER BY installed_rank DESC LIMIT 1
                     """)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private static String withoutCurrentSchema(String url) {
        int separator = url.indexOf('?');
        if (separator < 0) return url;
        String base = url.substring(0, separator);
        String parameters = url.substring(separator + 1).lines().findFirst().orElse("")
                .replaceAll("(^|&)currentSchema=[^&]*&?", "")
                .replaceAll("&&", "&")
                .replaceAll("(^&|&$)", "");
        return parameters.isBlank() ? base : base + "?" + parameters;
    }
}
