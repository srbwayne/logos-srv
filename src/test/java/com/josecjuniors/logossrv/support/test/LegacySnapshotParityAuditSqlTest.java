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

    private String classification(java.util.List<java.util.Map<String, Object>> rows, java.util.UUID id) {
        return rows.stream()
                .filter(row -> "DETAIL".equals(row.get("row_type")) && id.equals(row.get("atividade_config_id")))
                .map(row -> row.get("metric").toString())
                .findFirst()
                .orElseThrow();
    }
}
