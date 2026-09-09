package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;

@Repository
class JdbcVersionedProgressionConfigurationStore implements VersionedProgressionConfigurationStore {
    private final JdbcTemplate jdbc;
    private final AtividadeConfigRepository configs;
    private final HabilidadeRepository habilidades;
    private final EntityManager entityManager;

    JdbcVersionedProgressionConfigurationStore(JdbcTemplate jdbc, AtividadeConfigRepository configs,
                                                HabilidadeRepository habilidades, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.configs = configs;
        this.habilidades = habilidades;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Optional<ResolvedProgressionConfiguration> resolveVersioned(ProgressionConfigurationReference reference) {
        UUID legacyId = reference.value();
        ConfigRow row = findConfig(legacyId).orElseGet(() -> configs.findById(new AtividadeConfigId(legacyId))
                .map(config -> {
                    entityManager.flush();
                    return snapshotConfiguration(config);
                })
                .flatMap(this::findConfigByVersion)
                .orElse(null));
        if (row == null) return Optional.empty();

        PolicyRow policy = currentPolicy().orElseGet(this::snapshotPolicy);
        return Optional.of(materialize(row, policy));
    }

    @Override
    @Transactional
    public Optional<ResolvedProgressionConfiguration> resolveExternal(ExternalProgressionConfigurationReference reference) {
        String sql = reference.revision() == null ? """
                SELECT v.id, v.base_xp, v.base_stress FROM progression_configuration_definition d
                JOIN progression_configuration_version v ON v.id = d.current_version_id
                WHERE d.logical_key = ?
                """ : """
                SELECT v.id, v.base_xp, v.base_stress FROM progression_configuration_definition d
                JOIN progression_configuration_version v ON v.definition_id = d.id
                WHERE d.logical_key = ? AND v.revision = ?
                """;
        Object[] args = reference.revision() == null ? new Object[]{reference.key()} : new Object[]{reference.key(), reference.revision()};
        var rows = jdbc.query(sql, (rs, n) -> new ConfigRow(rs.getObject("id", UUID.class), rs.getInt("base_xp"), rs.getInt("base_stress")), args);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(materialize(rows.get(0), currentPolicy().orElseGet(this::snapshotPolicy)));
    }

    private ResolvedProgressionConfiguration materialize(ConfigRow row, PolicyRow policy) {
        var distributions = jdbc.query("""
                SELECT id, attribute_key, weight FROM progression_configuration_version_distribution
                WHERE configuration_version_id = ? ORDER BY id
                """, this::distribution, row.versionId);
        var configuration = new ProgressionConfiguration(row.baseXp, row.baseStress,
                distributions.stream().map(d -> toDistribution(d, row.versionId)).toList(),
                jdbc.query("""
                    SELECT skill_key, attribute_key, distribution_weight
                    FROM progression_skill_policy_version_rule
                    WHERE policy_version_id = ? ORDER BY id
                    """, (rs, n) -> new ProgressionConfiguration.SkillBonusRule(
                        rs.getString("skill_key"), rs.getString("attribute_key"), rs.getDouble("distribution_weight")),
                    policy.versionId));
        Set<String> numeric = new HashSet<>(jdbc.query("""
                SELECT factor_key FROM progression_configuration_version_factor
                WHERE configuration_version_id = ? AND tipo_input = 'NUMERICO'
                """, (rs, n) -> rs.getString(1), row.versionId));
        return new ResolvedProgressionConfiguration(row.versionId, policy.versionId, configuration, numeric);
    }

    private ProgressionConfiguration.AttributeDistribution toDistribution(DistributionRow d, UUID versionId) {
        var xp = jdbc.query("""
                SELECT factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode
                FROM progression_configuration_version_xp_rule WHERE distribution_id = ? ORDER BY id
                """, (rs, n) -> new ProgressionConfiguration.XpRule(rs.getString("factor_key"),
                        (Double) rs.getObject("multiplier"), (Double) rs.getObject("min_cutoff"),
                        (Double) rs.getObject("max_cutoff"), XpCalculationMode.valueOf(rs.getString("calculation_mode"))), d.id);
        var stress = jdbc.query("""
                SELECT multiplier, min_cutoff, max_cutoff, type
                FROM progression_configuration_version_stress_rule WHERE distribution_id = ? ORDER BY id
                """, (rs, n) -> new ProgressionConfiguration.StressRule(rs.getDouble("multiplier"),
                        (Double) rs.getObject("min_cutoff"), (Double) rs.getObject("max_cutoff"),
                        "NEGATIVE".equals(rs.getString("type")) ? ProgressionConfiguration.StressType.NEGATIVE
                                : ProgressionConfiguration.StressType.POSITIVE), d.id);
        return new ProgressionConfiguration.AttributeDistribution(d.attributeKey, d.weight, xp, stress);
    }

    private Optional<ConfigRow> findConfig(UUID legacyId) {
        return jdbc.query("""
                SELECT v.id, v.base_xp, v.base_stress
                FROM progression_configuration_definition d
                JOIN progression_configuration_version v ON v.id = d.current_version_id
                WHERE d.legacy_atividade_config_id = ?
                """, (rs, n) -> new ConfigRow(rs.getObject("id", UUID.class), rs.getInt("base_xp"),
                        rs.getInt("base_stress")), legacyId).stream().findFirst();
    }

    private Optional<ConfigRow> findConfigByVersion(UUID versionId) {
        return jdbc.query("SELECT id, base_xp, base_stress FROM progression_configuration_version WHERE id = ?",
                (rs, n) -> new ConfigRow(rs.getObject("id", UUID.class), rs.getInt("base_xp"), rs.getInt("base_stress")), versionId)
                .stream().findFirst();
    }

    private UUID snapshotConfiguration(AtividadeConfig config) {
        ensureSemanticKeysAvailable(config);
        UUID legacyId = config.getId().getValue();
        UUID definition = jdbc.query("SELECT id FROM progression_configuration_definition WHERE legacy_atividade_config_id = ?",
                (rs, n) -> rs.getObject(1, UUID.class), legacyId).stream().findFirst().orElseGet(() -> {
                    UUID id = UUID.randomUUID();
                    jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id) VALUES (?, ?, ?)",
                            id, "legacy:" + legacyId, legacyId);
                    return id;
                });
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM progression_configuration_version WHERE definition_id = ?",
                Integer.class, definition);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress) VALUES (?, ?, ?, ?, ?)",
                version, definition, revision, config.getXpBase(), config.getEstresseBase());
        for (RegraDistribuicaoAtividade d : config.getRegrasDistribuicao()) {
            UUID distribution = UUID.randomUUID();
            jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) VALUES (?, ?, ?, ?)",
                    distribution, version, d.getAtributo().getId().getValue().toString(), d.getPesoPercentual());
            for (RegraFatorXP r : d.getRegraFatorXPS()) {
                jdbc.update("INSERT INTO progression_configuration_version_xp_rule(id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), distribution, requiredSemanticKey(r.getFatorCalculo().getSemanticKey()), r.getPesoMultiplicador(), r.getPontoCorteMin(), r.getPontoCorteMax(), XpCalculationMode.FIXED.name());
            }
            for (RegraFatorEstresse r : d.getRegraFatorEstresses()) {
                jdbc.update("INSERT INTO progression_configuration_version_stress_rule(id, distribution_id, multiplier, min_cutoff, max_cutoff, type) VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), distribution, r.getPesoMultiplicador(), r.getPontoCorteMin(), r.getPontoCorteMax(), r.getTipo().name());
            }
        }
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) SELECT gen_random_uuid(), ?, semantic_key, tipo_input FROM fator_calculo WHERE semantic_key IS NOT NULL", version);
        jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ? WHERE id = ?", version, definition);
        return version;
    }

    private String requiredSemanticKey(String semanticKey) {
        if (semanticKey == null || semanticKey.isBlank()) {
            throw new IllegalStateException("Todos os fatores referenciados por uma nova versão devem possuir semanticKey.");
        }
        return semanticKey;
    }

    private void ensureSemanticKeysAvailable(AtividadeConfig config) {
        config.getRegrasDistribuicao().stream()
                .flatMap(distribution -> distribution.getRegraFatorXPS().stream())
                .map(rule -> rule.getFatorCalculo().getSemanticKey())
                .filter(java.util.Objects::isNull)
                .findAny()
                .ifPresent(missing -> {
                    throw new IllegalStateException("Novas versões de configuração exigem semanticKey nos fatores referenciados.");
                });
    }

    @Override
    @Transactional
    public void snapshotConfiguration(UUID legacyId) {
        configs.findById(new AtividadeConfigId(legacyId)).ifPresent(this::snapshotConfiguration);
    }

    @Override
    @Transactional
    public void snapshotSkillPolicy() {
        snapshotPolicy();
    }

    private Optional<PolicyRow> currentPolicy() {
        return jdbc.query("SELECT current_version_id FROM progression_skill_policy WHERE logical_key = 'global' AND current_version_id IS NOT NULL",
                (rs, n) -> new PolicyRow(rs.getObject(1, UUID.class))).stream().findFirst();
    }

    private PolicyRow snapshotPolicy() {
        UUID policy = jdbc.query("SELECT id FROM progression_skill_policy WHERE logical_key = 'global'",
                (rs, n) -> rs.getObject(1, UUID.class)).stream().findFirst().orElseGet(() -> {
                    UUID id = UUID.randomUUID();
                    jdbc.update("INSERT INTO progression_skill_policy(id, logical_key) VALUES (?, 'global')", id);
                    return id;
                });
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM progression_skill_policy_version WHERE policy_id = ?", Integer.class, policy);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_skill_policy_version(id, policy_id, revision) VALUES (?, ?, ?)", version, policy, revision);
        habilidades.findAll().forEach(skill -> skill.getRegrasDistribuicao().forEach(rule -> jdbc.update(
                "INSERT INTO progression_skill_policy_version_rule(id, policy_version_id, skill_key, attribute_key, distribution_weight) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), version, skill.getId().getValue().toString(), rule.getAtributo().getId().getValue().toString(), rule.getPesoDistribuicao())));
        jdbc.update("UPDATE progression_skill_policy SET current_version_id = ? WHERE id = ?", version, policy);
        return new PolicyRow(version);
    }

    private DistributionRow distribution(ResultSet rs, int ignored) throws java.sql.SQLException {
        return new DistributionRow(rs.getObject("id", UUID.class), rs.getString("attribute_key"), rs.getDouble("weight"));
    }

    private record ConfigRow(UUID versionId, int baseXp, int baseStress) {}
    private record PolicyRow(UUID versionId) {}
    private record DistributionRow(UUID id, String attributeKey, double weight) {}
}
