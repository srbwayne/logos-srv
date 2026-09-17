package com.josecjuniors.logossrv.support.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class LegacyActivityProgressionSchemaRetirementTest {

    private static final List<String> RETIRED_TABLES = List.of(
            "regra_fator_xp",
            "regra_fator_estresse",
            "regra_distribuicao_atividade");

    private static final List<String> RETIRED_COLUMNS = List.of(
            "xp_base",
            "estresse_base",
            "dias_para_penalidade",
            "xp_perda_por_ciclo");

    private static final List<String> REQUIRED_TABLES = List.of(
            "atividade_config",
            "atividade_formulario",
            "atividade_agendada",
            "registro_atividade",
            "fator_calculo",
            "atributo",
            "regra_distribuicao_habilidade",
            "progression_configuration_definition",
            "progression_configuration_version",
            "progression_configuration_version_distribution",
            "progression_configuration_version_xp_rule",
            "progression_configuration_version_stress_rule",
            "progression_configuration_version_factor",
            "progression_configuration_draft",
            "progression_configuration_draft_factor",
            "progression_configuration_draft_distribution",
            "progression_configuration_draft_xp_rule",
            "progression_configuration_draft_stress_rule",
            "progression_skill_policy",
            "progression_skill_policy_version",
            "progression_skill_policy_version_rule",
            "progression_external_execution");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v42RetiresLegacyActivityStorageAndPreservesModernSchema() {
        assertThat(jdbc.queryForObject("SELECT version()", String.class))
                .contains("PostgreSQL 16");
        assertThat(jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history "
                        + "WHERE success = true ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("43");

        for (String table : RETIRED_TABLES) {
            assertThat(tableExists(table)).as("retired table %s", table).isFalse();
        }

        for (String column : RETIRED_COLUMNS) {
            assertThat(columnExists("atividade_config", column))
                    .as("retired column atividade_config.%s", column)
                    .isFalse();
        }

        for (String table : REQUIRED_TABLES) {
            assertThat(tableExists(table)).as("required table %s", table).isTrue();
        }

        assertThat(columnExists(
                "progression_configuration_definition", "legacy_atividade_config_id"))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE constraint_schema = current_schema() "
                        + "AND constraint_name = 'fk_progression_configuration_definition_legacy'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT delete_rule FROM information_schema.referential_constraints "
                        + "WHERE constraint_schema = current_schema() "
                        + "AND constraint_name = 'fk_progression_configuration_definition_legacy'",
                String.class)).isEqualTo("SET NULL");
    }

    private boolean tableExists(String table) {
        return jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?)",
                Boolean.class,
                table);
    }

    private boolean columnExists(String table, String column) {
        return jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() "
                        + "AND table_name = ? AND column_name = ?)",
                Boolean.class,
                table,
                column);
    }
}
