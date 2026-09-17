package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionTemporalCutoverPostgresTest {
    private static final String SOURCE = "task039p";
    private static final String CONFIG_PREFIX = "task039p-";

    @Autowired JdbcTemplate jdbc;
    @Autowired ProgressionExternalExecutionJpaRepository executions;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanScratchObjects() {
        jdbc.execute("DROP TABLE IF EXISTS temporal_order_probe");
        jdbc.execute("DROP TABLE IF EXISTS temporal_probe");
        cleanupFixtures();
    }

    @AfterEach
    void cleanFixtures() {
        cleanupFixtures();
        jdbc.execute("DROP TABLE IF EXISTS temporal_order_probe");
        jdbc.execute("DROP TABLE IF EXISTS temporal_probe");
    }

    @Test
    void provesNullableColumnDefaultAndPostgresType() {
        jdbc.execute("CREATE TABLE temporal_probe (id INTEGER PRIMARY KEY, created_at TIMESTAMP NOT NULL)");
        jdbc.update("INSERT INTO temporal_probe(id, created_at) VALUES (?, TIMESTAMP '2026-01-01 10:00:00')", 1);

        jdbc.execute("ALTER TABLE temporal_probe ADD COLUMN occurred_at TIMESTAMPTZ");
        assertThat(occurredAt(1)).isNull();

        jdbc.execute("ALTER TABLE temporal_probe ALTER COLUMN occurred_at SET DEFAULT CURRENT_TIMESTAMP");
        assertThat(occurredAt(1)).isNull();

        jdbc.update("INSERT INTO temporal_probe(id, created_at) VALUES (?, TIMESTAMP '2026-01-02 10:00:00')", 2);
        assertThat(occurredAt(2)).isNotNull();

        jdbc.update("INSERT INTO temporal_probe(id, created_at, occurred_at) VALUES (?, TIMESTAMP '2026-01-03 10:00:00', NULL)", 3);
        assertThat(occurredAt(3)).isNull();

        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_name = 'temporal_probe' AND column_name = 'occurred_at'", String.class))
                .isEqualTo("timestamp with time zone");
    }

    @Test
    void databaseGeneratesOccurredAtAndUpdatesDoNotChangeIt() {
        var refs = refs();
        var id = UUID.randomUUID();
        var entity = new ProgressionExternalExecutionEntity(id, SOURCE, "jpa-" + id,
                "f".repeat(64), "{}", "{}", "PENDING", 0, null,
                "task039p", "subject-1", refs.configurationKey, 1,
                refs.configurationVersionId, refs.skillPolicyVersionId);

        var saved = executions.saveAndFlush(entity);
        assertThat(saved.getOccurredAt()).as("database-generated value is not populated in memory without refresh").isNull();

        Instant original = occurredAt(id);
        assertThat(original).isNotNull();

        entityManager.clear();
        var reloaded = executions.findById(id).orElseThrow();
        assertThat(reloaded.getOccurredAt()).isNotNull();
        assertThat(reloaded.getOccurredAt()).isEqualTo(original);

        reloaded.markAttempt("PROCESSING", null);
        executions.saveAndFlush(reloaded);
        entityManager.clear();
        assertThat(executions.findById(id).orElseThrow().getOccurredAt()).isEqualTo(original);

        var completing = executions.findById(id).orElseThrow();
        completing.markCompleted("{}");
        executions.saveAndFlush(completing);
        entityManager.clear();
        assertThat(executions.findById(id).orElseThrow().getOccurredAt()).isEqualTo(original);
    }

    @Test
    void legacyNullOccurredAtReloadsWithoutInferringFromCreatedAt() {
        var refs = refs();
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO progression_external_execution
                    (id, source_system, idempotency_key, request_fingerprint, response_json,
                     request_json, processing_status, attempt_count, last_error,
                     subject_namespace, subject_external_id, configuration_key, requested_revision,
                     configuration_version_id, skill_policy_version_id, created_at, occurred_at)
                VALUES (?, ?, ?, ?, '{}', '{}', 'COMPLETED', 1, NULL, ?, ?, ?, 1, ?, ?, ?, NULL)
                """, id, SOURCE, "legacy-" + id, "f".repeat(64),
                "task039p", "legacy-subject", refs.configurationKey, refs.configurationVersionId,
                refs.skillPolicyVersionId, LocalDateTime.of(2026, 1, 1, 10, 0));

        assertThat(executions.findById(id).orElseThrow().getOccurredAt()).isNull();
    }

    @Test
    void serializesInstantAsUtcIso8601() throws Exception {
        var json = objectMapper.writeValueAsString(new OccurredAtResponse(Instant.parse("2026-09-17T01:23:45Z")));
        assertThat(json).isEqualTo("{\"occurredAt\":\"2026-09-17T01:23:45Z\"}");
    }

    @Test
    void ordersCanonicalRowsBeforeLegacyRowsAndUsesUuidAsTieBreaker() {
        jdbc.execute("CREATE TABLE temporal_order_probe (id UUID PRIMARY KEY, created_at TIMESTAMP NOT NULL, occurred_at TIMESTAMPTZ)");
        var canonicalOlder = UUID.fromString("00000000-0000-4000-8000-000000000001");
        var canonicalNewer = UUID.fromString("00000000-0000-4000-8000-000000000002");
        var legacyOlder = UUID.fromString("00000000-0000-4000-8000-000000000003");
        var legacyNewer = UUID.fromString("00000000-0000-4000-8000-000000000004");
        var legacyTieHigh = UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff");
        var legacyTieLow = UUID.fromString("00000000-0000-4000-8000-000000000005");

        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-01 10:00:00', TIMESTAMPTZ '2026-02-01 10:00:00+00')", canonicalOlder);
        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-01 11:00:00', TIMESTAMPTZ '2026-02-02 10:00:00+00')", canonicalNewer);
        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-03 10:00:00', NULL)", legacyOlder);
        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-04 10:00:00', NULL)", legacyNewer);
        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-05 10:00:00', NULL)", legacyTieHigh);
        jdbc.update("INSERT INTO temporal_order_probe VALUES (?, TIMESTAMP '2026-01-05 10:00:00', NULL)", legacyTieLow);

        var ids = jdbc.queryForList("""
                SELECT id FROM temporal_order_probe
                ORDER BY occurred_at DESC NULLS LAST, created_at DESC, id DESC
                """, UUID.class);

        assertThat(ids).containsExactly(canonicalNewer, canonicalOlder, legacyTieHigh, legacyTieLow, legacyNewer, legacyOlder);
    }

    private Instant occurredAt(int id) {
        var value = jdbc.queryForObject("SELECT occurred_at FROM temporal_probe WHERE id = ?", OffsetDateTime.class, id);
        return value == null ? null : value.toInstant();
    }

    private Instant occurredAt(UUID id) {
        var value = jdbc.queryForObject("SELECT occurred_at FROM progression_external_execution WHERE id = ?", OffsetDateTime.class, id);
        return value == null ? null : value.toInstant();
    }

    private Refs refs() {
        var definition = UUID.randomUUID();
        var version = UUID.randomUUID();
        var policy = UUID.randomUUID();
        var policyVersion = UUID.randomUUID();
        var key = CONFIG_PREFIX + definition;
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, NULL, NULL)", definition, key);
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 0, 0, 'SEMANTIC')", version, definition);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", version, definition);
        jdbc.update("INSERT INTO progression_skill_policy(id, logical_key, current_version_id) VALUES (?, ?, NULL)", policy, CONFIG_PREFIX + "policy-" + policy);
        jdbc.update("INSERT INTO progression_skill_policy_version(id, policy_id, revision) VALUES (?, ?, 1)", policyVersion, policy);
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?", policyVersion, policy);
        return new Refs(key, version, policyVersion);
    }

    private void cleanupFixtures() {
        jdbc.update("DELETE FROM progression_external_execution WHERE source_system = ?", SOURCE);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = NULL WHERE logical_key LIKE ?", CONFIG_PREFIX + "%");
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = NULL WHERE logical_key LIKE ?", CONFIG_PREFIX + "%");
        jdbc.update("DELETE FROM progression_configuration_version WHERE definition_id IN (SELECT id FROM progression_configuration_definition WHERE logical_key LIKE ?)", CONFIG_PREFIX + "%");
        jdbc.update("DELETE FROM progression_configuration_definition WHERE logical_key LIKE ?", CONFIG_PREFIX + "%");
        jdbc.update("DELETE FROM progression_skill_policy_version WHERE policy_id IN (SELECT id FROM progression_skill_policy WHERE logical_key LIKE ?)", CONFIG_PREFIX + "%");
        jdbc.update("DELETE FROM progression_skill_policy WHERE logical_key LIKE ?", CONFIG_PREFIX + "%");
    }

    private record Refs(String configurationKey, UUID configurationVersionId, UUID skillPolicyVersionId) {}
    private record OccurredAtResponse(Instant occurredAt) {}
}
