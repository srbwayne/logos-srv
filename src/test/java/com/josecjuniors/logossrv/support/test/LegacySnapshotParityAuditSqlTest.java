package com.josecjuniors.logossrv.support.test;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Validates the checked-in audit SQL against the real Flyway V41 test schema. */
@IntegrationTest
class LegacySnapshotParityAuditSqlTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    private static final Path AUDIT = Path.of(
            "docs/audits/task-034b-legacy-snapshot-parity.sql");

    @Test
    void auditScriptExecutesReadOnlyAgainstV41Schema() throws Exception {
        Path auditPath = AUDIT.toAbsolutePath().normalize();
        assertThat(auditPath).exists();

        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.createStatement().execute("SET TRANSACTION READ ONLY");
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new FileSystemResource(auditPath.toFile())));
            connection.rollback();
        }
    }

    @Test
    void auditModelsResolverCandidatesAndBothSnapshotGenerations() throws Exception {
        String sql = Files.readString(AUDIT.toAbsolutePath().normalize());

        assertThat(sql).contains("RUNTIME_RESOLVER_CANDIDATES_TOTAL");
        assertThat(sql).contains("UNCONFIGURED_RUNTIME_CANDIDATE");
        assertThat(sql).contains("INCOMPLETE_LEGACY_BASES");
        assertThat(sql).contains("LEGACY_UUID");
        assertThat(sql).contains("SEMANTIC");
        assertThat(sql).contains("SEMANTIC_KEY_UNREPRESENTABLE");
        assertThat(sql).contains("HISTORICAL_DETACHED");
        assertThat(sql).doesNotContain("THEN 'RUNTIME_RELEVANT'");
        assertThat(sql).doesNotContain("WHERE reachability = 'RUNTIME_RELEVANT'");
    }

    @Test
    void auditIncludesUnregisteredCandidatesAndClassifiesEmptyAndBothGenerations() throws Exception {
        var deterministic = java.util.UUID.randomUUID();
        var empty = java.util.UUID.randomUUID();
        var legacy = java.util.UUID.randomUUID();
        var semantic = java.util.UUID.randomUUID();
        var legacyDefinition = java.util.UUID.randomUUID();
        var semanticDefinition = java.util.UUID.randomUUID();
        var legacyVersion = java.util.UUID.randomUUID();
        var semanticVersion = java.util.UUID.randomUUID();

        jdbc.update("INSERT INTO atividade_config(id, nome, xp_base, estresse_base) VALUES (?, ?, ?, ?)",
                deterministic, "audit-deterministic", 10, 20);
        jdbc.update("INSERT INTO atividade_config(id, nome, xp_base, estresse_base) VALUES (?, ?, ?, ?)",
                empty, "audit-empty", null, null);
        jdbc.update("INSERT INTO atividade_config(id, nome, xp_base, estresse_base) VALUES (?, ?, ?, ?)",
                legacy, "audit-legacy", 10, 20);
        jdbc.update("INSERT INTO atividade_config(id, nome, xp_base, estresse_base) VALUES (?, ?, ?, ?)",
                semantic, "audit-semantic", 10, 20);

        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id) VALUES (?, ?, ?)",
                legacyDefinition, "audit-legacy-definition", legacy);
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id) VALUES (?, ?, ?)",
                semanticDefinition, "audit-semantic-definition", semantic);
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 10, 20, 'LEGACY_UUID')",
                legacyVersion, legacyDefinition);
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 10, 20, 'SEMANTIC')",
                semanticVersion, semanticDefinition);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?",
                legacyVersion, legacyDefinition);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?",
                semanticVersion, semanticDefinition);

        String sql = Files.readString(AUDIT.toAbsolutePath().normalize());
        String firstResult = sql.substring(0, sql.indexOf("-- Result 2:"));
        var rows = jdbc.queryForList(firstResult.replaceFirst(";\\s*$", ""));

        assertThat(rows.stream().filter(row -> "DETAIL".equals(row.get("row_type"))))
                .hasSize(4);
        assertThat(classification(rows, deterministic)).isEqualTo("NEEDS_BACKFILL");
        assertThat(classification(rows, empty)).isEqualTo("AMBIGUOUS");
        assertThat(classification(rows, legacy)).isEqualTo("SAFE_FROZEN");
        assertThat(classification(rows, semantic)).isEqualTo("SAFE_FROZEN");
        assertThat(rows.stream().filter(row -> "RUNTIME_RESOLVER_CANDIDATES_TOTAL".equals(row.get("metric")))
                .map(row -> row.get("value").toString())).containsExactly("4");
    }

    @Test
    void auditRegressionMatrixCoversNonEmptyGraphsStalenessReferencesAndDetachedHistory() throws Exception {
        Fixture legacy = fixture("legacy", false, true);
        Fixture semantic = fixture("semantic", true, true);
        Fixture fallback = fixture("fallback", false, false);
        UUID empty = activity("empty-matrix", null, null);
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) VALUES (?, ?, ?, 'NUMERICO')",
                UUID.randomUUID(), legacy.version, semantic.factor.toString());

        assertClassification(legacy.activity, "SAFE_FROZEN");
        assertClassification(semantic.activity, "SAFE_FROZEN");
        assertClassification(fallback.activity, "NEEDS_BACKFILL");
        assertClassification(empty, "AMBIGUOUS", "UNCONFIGURED_RUNTIME_CANDIDATE");

        jdbc.update("UPDATE atividade_config SET xp_base = NULL WHERE id = ?", fallback.activity);
        assertClassification(fallback.activity, "AMBIGUOUS", "INCOMPLETE_LEGACY_BASES");
        jdbc.update("UPDATE atividade_config SET xp_base = 10, dias_para_penalidade = 3 WHERE id = ?", empty);
        assertClassification(empty, "AMBIGUOUS", "UNCONFIGURED_RUNTIME_CANDIDATE");
        assertSummary("PENALTY_METADATA_ROWS", "1");

        jdbc.update("UPDATE progression_configuration_version SET base_xp = 11 WHERE id = ?", legacy.version);
        assertClassification(legacy.activity, "NEEDS_BACKFILL", "STALE_SCALAR");
        jdbc.update("UPDATE progression_configuration_version SET base_xp = 10 WHERE id = ?", legacy.version);
        jdbc.update("UPDATE progression_configuration_version_distribution SET weight = 99 WHERE id = ?", legacy.distributionVersion);
        assertClassification(legacy.activity, "NEEDS_BACKFILL", "STALE_DISTRIBUTION");
        jdbc.update("UPDATE progression_configuration_version_distribution SET weight = 50 WHERE id = ?", legacy.distributionVersion);
        jdbc.update("UPDATE progression_configuration_version_xp_rule SET multiplier = 9 WHERE id = ?", legacy.xpVersion);
        assertClassification(legacy.activity, "NEEDS_BACKFILL", "STALE_XP_RULE");
        jdbc.update("UPDATE progression_configuration_version_xp_rule SET multiplier = 2 WHERE id = ?", legacy.xpVersion);
        jdbc.update("UPDATE progression_configuration_version_stress_rule SET multiplier = 9 WHERE id = ?", legacy.stressVersion);
        assertClassification(legacy.activity, "NEEDS_BACKFILL", "STALE_STRESS_RULE");
        jdbc.update("UPDATE progression_configuration_version_stress_rule SET multiplier = 1.5 WHERE id = ?", legacy.stressVersion);
        jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id = ?", legacy.version);
        assertClassification(legacy.activity, "NEEDS_BACKFILL", "STALE_FACTOR_SNAPSHOT");

        jdbc.update("UPDATE fator_calculo SET semantic_key = NULL WHERE id = ?", semantic.factor);
        assertClassification(semantic.activity, "AMBIGUOUS", "SEMANTIC_KEY_UNREPRESENTABLE");

        Fixture semanticStale = fixture("semantic-stale", true, true);
        jdbc.update("UPDATE progression_configuration_version_xp_rule SET multiplier = 8 WHERE id = ?", semanticStale.xpVersion);
        assertClassification(semanticStale.activity, "NEEDS_BACKFILL", "STALE_XP_RULE");
        jdbc.update("DELETE FROM progression_configuration_version_factor WHERE configuration_version_id = ?", semanticStale.version);
        assertClassification(semanticStale.activity, "NEEDS_BACKFILL", "STALE_FACTOR_SNAPSHOT");

        Fixture ownerA = fixture("owner-a", false, true);
        UUID ownerBDefinition = UUID.randomUUID();
        UUID ownerBVersion = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key) VALUES (?, ?)",
                ownerBDefinition, "audit-owner-b");
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 10, 20, 'LEGACY_UUID')",
                ownerBVersion, ownerBDefinition);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?",
                ownerBVersion, ownerA.definition);
        assertClassification(ownerA.activity, "BROKEN_REFERENCE", "VERSION_OWNER_MISMATCH");

        Fixture duplicate = fixture("duplicate", false, true);
        UUID duplicateDistribution = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) VALUES (?, ?, ?, ?)",
                duplicateDistribution, duplicate.version, duplicate.attribute.toString(), 50);
        assertClassification(duplicate.activity, "AMBIGUOUS", "DUPLICATE_RULE");

        Fixture durable = fixture("durable", false, true);
        String durableClassificationBeforeReference = String.valueOf(
                detail(jdbc.queryForList(firstResult()), durable.activity).get("metric"));
        UUID user = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID registro = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id, email, password) VALUES (?, ?, ?)", user, "audit-" + user + "@example.test", "x");
        jdbc.update("INSERT INTO jogador(id, user_id, nome_exibicao) VALUES (?, ?, ?)", player, user, "audit-player");
        jdbc.update("INSERT INTO registro_atividade(id, jogador_id, atividade_config_id, data_hora_inicio, situacao, status_processamento, data_registro, configuration_version_id) VALUES (?, ?, ?, now(), 'CONCLUIDA', 'PROCESSADO', now(), ?)",
                registro, player, durable.activity, durable.version);
        UUID policyVersion = jdbc.queryForObject("SELECT current_version_id FROM progression_skill_policy WHERE logical_key = 'global'", UUID.class);
        jdbc.update("INSERT INTO progression_external_execution(id, source_system, idempotency_key, request_fingerprint, subject_namespace, subject_external_id, configuration_key, requested_revision, configuration_version_id, skill_policy_version_id, response_json, created_at, request_json, processing_status, attempt_count) VALUES (?, 'audit', ?, ?, 'audit', ?, 'audit', 1, ?, ?, '{}', now(), '{}', 'COMPLETED', 1)",
                UUID.randomUUID(), "key-" + UUID.randomUUID(), "fingerprint", player.toString(), durable.version, policyVersion);
        assertClassification(durable.activity, "SAFE_FROZEN");
        assertThat(String.valueOf(detail(jdbc.queryForList(firstResult()), durable.activity).get("metric")))
                .isEqualTo(durableClassificationBeforeReference);
        Map<String, Object> durableRow = jdbc.queryForList("SELECT v.id AS configuration_version_id, COUNT(DISTINCT e.id) AS external_execution_reference_count, COUNT(DISTINCT ra.id) AS activity_execution_reference_count, COUNT(DISTINCT e.id) + COUNT(DISTINCT ra.id) AS total_durable_reference_count FROM progression_configuration_version v JOIN progression_configuration_definition d ON d.id = v.definition_id LEFT JOIN progression_external_execution e ON e.configuration_version_id = v.id LEFT JOIN registro_atividade ra ON ra.configuration_version_id = v.id WHERE v.id = ? GROUP BY v.id", durable.version).get(0);
        assertThat(durableRow).containsEntry("external_execution_reference_count", 1L)
                .containsEntry("activity_execution_reference_count", 1L)
                .containsEntry("total_durable_reference_count", 2L);

        UUID detachedDefinition = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key) VALUES (?, ?)", detachedDefinition, "audit-detached");
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 10, 20, 'LEGACY_UUID')", UUID.randomUUID(), detachedDefinition);
        assertSummaryAtLeast("HISTORICAL_DETACHED", 1);
    }

    private String classification(java.util.List<java.util.Map<String, Object>> rows, java.util.UUID id) {
        return rows.stream()
                .filter(row -> "DETAIL".equals(row.get("row_type")) && id.equals(row.get("atividade_config_id")))
                .map(row -> row.get("metric").toString())
                .findFirst()
                .orElseThrow();
    }

    private void assertClassification(UUID activity, String expected, String... reasons) throws Exception {
        var row = detail(jdbc.queryForList(firstResult()), activity);
        assertThat(row.get("metric")).isEqualTo(expected);
        String actualReasons = String.valueOf(row.get("reasons"));
        for (String reason : reasons) assertThat(actualReasons).contains(reason);
    }

    private void assertSummary(String metric, String value) throws Exception {
        assertThat(jdbc.queryForList(firstResult()).stream()
                .filter(row -> metric.equals(row.get("metric")))
                .map(row -> String.valueOf(row.get("value")))).contains(value);
    }

    private void assertSummaryAtLeast(String metric, int minimum) throws Exception {
        int value = jdbc.queryForList(firstResult()).stream()
                .filter(row -> metric.equals(row.get("metric")))
                .map(row -> Integer.parseInt(String.valueOf(row.get("value"))))
                .findFirst().orElse(0);
        assertThat(value).isGreaterThanOrEqualTo(minimum);
    }

    private Map<String, Object> detail(java.util.List<java.util.Map<String, Object>> rows, UUID id) {
        return rows.stream()
                .filter(row -> "DETAIL".equals(row.get("row_type"))
                        && id.toString().equals(String.valueOf(row.get("atividade_config_id"))))
                .findFirst().orElseThrow();
    }

    private String firstResult() throws Exception {
        String sql = Files.readString(AUDIT.toAbsolutePath().normalize());
        return sql.substring(0, sql.indexOf("-- Result 2:")).replaceFirst(";\\s*$", "");
    }

    private UUID activity(String name, Integer xpBase, Integer stressBase) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO atividade_config(id, nome, xp_base, estresse_base) VALUES (?, ?, ?, ?)", id, "audit-" + name, xpBase, stressBase);
        return id;
    }

    private Fixture fixture(String name, boolean semantic, boolean withVersion) {
        UUID activity = activity(name, 10, 20);
        UUID attribute = UUID.randomUUID();
        UUID factor = UUID.randomUUID();
        UUID distribution = UUID.randomUUID();
        UUID xp = UUID.randomUUID();
        UUID stress = UUID.randomUUID();
        jdbc.update("INSERT INTO atributo(id, nome) VALUES (?, ?)", attribute, "audit-attribute-" + name);
        jdbc.update("INSERT INTO fator_calculo(id, nome, unidade_medida, tipo_input, semantic_key) VALUES (?, ?, 'u', 'NUMERICO', ?)",
                factor, "audit-factor-" + name, semantic ? "audit.factor." + name : null);
        jdbc.update("INSERT INTO regra_distribuicao_atividade(id, atividade_config_id, atributo_id, peso_percentual) VALUES (?, ?, ?, 50)",
                distribution, activity, attribute);
        jdbc.update("INSERT INTO regra_fator_xp(id, regra_distribuicao_atividade_id, fator_calculo_id, peso_multiplicador, ponto_corte_min, ponto_corte_max) VALUES (?, ?, ?, 2, 0, 100)",
                xp, distribution, factor);
        jdbc.update("INSERT INTO regra_fator_estresse(id, regra_distribuicao_atividade_id, peso_multiplicador, ponto_corte_min, ponto_corte_max, tipo) VALUES (?, ?, 1.5, 0, 100, 'POSITIVO')",
                stress, distribution);
        if (!withVersion) return new Fixture(activity, attribute, factor, distribution, xp, stress, null, null, null);

        UUID definition = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id) VALUES (?, ?, ?)",
                definition, "audit-definition-" + name, activity);
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation) VALUES (?, ?, 1, 10, 20, ?)",
                version, definition, semantic ? "SEMANTIC" : "LEGACY_UUID");
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", version, definition);
        UUID distributionVersion = UUID.randomUUID();
        UUID xpVersion = UUID.randomUUID();
        UUID stressVersion = UUID.randomUUID();
        String factorKey = semantic ? "audit.factor." + name : factor.toString();
        jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) VALUES (?, ?, ?, 50)",
                distributionVersion, version, attribute.toString());
        jdbc.update("INSERT INTO progression_configuration_version_xp_rule(id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode) VALUES (?, ?, ?, 2, 0, 100, 'FIXED')",
                xpVersion, distributionVersion, factorKey);
        jdbc.update("INSERT INTO progression_configuration_version_stress_rule(id, distribution_id, multiplier, min_cutoff, max_cutoff, type) VALUES (?, ?, 1.5, 0, 100, 'POSITIVO')",
                stressVersion, distributionVersion);
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) VALUES (?, ?, ?, 'NUMERICO')",
                UUID.randomUUID(), version, factorKey);
        return new Fixture(activity, attribute, factor, distribution, xp, stress, definition, version, distributionVersion, xpVersion, stressVersion);
    }

    private record Fixture(UUID activity, UUID attribute, UUID factor, UUID distribution, UUID xp, UUID stress,
                           UUID definition, UUID version, UUID distributionVersion, UUID xpVersion, UUID stressVersion) {
        private Fixture(UUID activity, UUID attribute, UUID factor, UUID distribution, UUID xp, UUID stress,
                        UUID definition, UUID version, UUID distributionVersion) {
            this(activity, attribute, factor, distribution, xp, stress, definition, version, distributionVersion, null, null);
        }
    }
}
