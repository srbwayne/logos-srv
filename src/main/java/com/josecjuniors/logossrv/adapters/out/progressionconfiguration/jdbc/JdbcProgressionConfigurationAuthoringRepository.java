package com.josecjuniors.logossrv.adapters.out.progressionconfiguration.jdbc;

import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository.ProgressionConfigurationAuthoringRepository;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationActivationConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProgressionConfigurationAuthoringRepository implements ProgressionConfigurationAuthoringRepository {
    private final JdbcTemplate jdbc;

    public JdbcProgressionConfigurationAuthoringRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ProgressionConfigurationDefinition create(String logicalKey) {
        UUID definition = UUID.randomUUID();
        UUID draft = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_definition(id, logical_key, legacy_atividade_config_id, current_version_id) VALUES (?, ?, NULL, NULL)", definition, logicalKey);
        jdbc.update("INSERT INTO progression_configuration_draft(id, definition_id, version, base_xp, base_stress) VALUES (?, ?, 0, NULL, NULL)", draft, definition);
        return new ProgressionConfigurationDefinition(definition, logicalKey, null, 0L, 0L, false);
    }

    @Override
    public Optional<ProgressionConfigurationDefinition> find(String logicalKey) {
        return jdbc.query("""
                SELECT d.id, d.logical_key, d.legacy_atividade_config_id,
                       v.revision, dr.version, d.activation_version
                FROM progression_configuration_definition d
                LEFT JOIN progression_configuration_version v ON v.id = d.current_version_id
                LEFT JOIN progression_configuration_draft dr ON dr.definition_id = d.id
                WHERE d.logical_key = ?
                """, (rs, n) -> new ProgressionConfigurationDefinition(
                rs.getObject("id", UUID.class), rs.getString("logical_key"),
                (Integer) rs.getObject("revision"), (Long) rs.getObject("version"),
                rs.getLong("activation_version"),
                rs.getObject("legacy_atividade_config_id") != null), logicalKey).stream().findFirst();
    }

    @Override
    public Optional<ProgressionConfigurationDraft> findDraft(String logicalKey) {
        Optional<ProgressionConfigurationDefinition> definition = find(logicalKey);
        if (definition.isEmpty()) return Optional.empty();
        UUID definitionId = definition.get().id();
        var roots = jdbc.query("SELECT id, version, base_xp, base_stress FROM progression_configuration_draft WHERE definition_id = ?", (rs, n) -> new DraftRoot(
                rs.getObject("id", UUID.class), rs.getLong("version"), (Integer) rs.getObject("base_xp"), (Integer) rs.getObject("base_stress")), definitionId);
        if (roots.isEmpty()) return Optional.empty();
        DraftRoot root = roots.get(0);
        List<String> factors = jdbc.query("""
                SELECT f.semantic_key FROM progression_configuration_draft_factor df
                JOIN fator_calculo f ON f.id = df.fator_calculo_id
                WHERE df.draft_id = ? ORDER BY df.id
                """, (rs, n) -> rs.getString(1), root.id);
        List<DistributionBuilder> builders = jdbc.query("SELECT id, attribute_id, weight FROM progression_configuration_draft_distribution WHERE draft_id = ? ORDER BY id", (rs, n) -> new DistributionBuilder(
                rs.getObject("id", UUID.class), rs.getObject("attribute_id", UUID.class), rs.getDouble("weight")), root.id);
        List<ProgressionConfigurationDraft.Distribution> distributions = new ArrayList<>();
        for (DistributionBuilder builder : builders) {
            List<ProgressionConfigurationDraft.XpRule> xp = jdbc.query("""
                    SELECT f.semantic_key, r.multiplier, r.min_cutoff, r.max_cutoff, r.calculation_mode
                    FROM progression_configuration_draft_xp_rule r JOIN fator_calculo f ON f.id = r.fator_calculo_id
                    WHERE r.distribution_id = ? ORDER BY r.id
                    """, (rs, n) -> new ProgressionConfigurationDraft.XpRule(rs.getString(1), (Double) rs.getObject(2), (Double) rs.getObject(3), (Double) rs.getObject(4), rs.getString(5) == null ? XpCalculationMode.FIXED : XpCalculationMode.valueOf(rs.getString(5))), builder.id);
            List<ProgressionConfigurationDraft.StressRule> stress = jdbc.query("SELECT multiplier, min_cutoff, max_cutoff, type FROM progression_configuration_draft_stress_rule WHERE distribution_id = ? ORDER BY id", (rs, n) -> new ProgressionConfigurationDraft.StressRule(rs.getDouble(1), (Double) rs.getObject(2), (Double) rs.getObject(3), rs.getString(4)), builder.id);
            distributions.add(new ProgressionConfigurationDraft.Distribution(builder.attributeId, builder.weight, xp, stress));
        }
        return Optional.of(new ProgressionConfigurationDraft(definitionId, root.version, root.baseXp, root.baseStress, factors, distributions));
    }

    @Override
    @Transactional
    public ProgressionConfigurationDraft replaceDraft(String logicalKey, long expectedVersion, ProgressionConfigurationDraft draft) {
        ProgressionConfigurationDefinition definition = find(logicalKey).orElseThrow();
        jdbc.queryForObject("SELECT id FROM progression_configuration_definition WHERE id = ? FOR UPDATE", UUID.class, definition.id());
        UUID draftId = jdbc.queryForObject("SELECT id FROM progression_configuration_draft WHERE definition_id = ?", UUID.class, definition.id());
        jdbc.queryForObject("SELECT id FROM progression_configuration_draft WHERE id = ? FOR UPDATE", UUID.class, draftId);
        int updated = jdbc.update("UPDATE progression_configuration_draft SET version = version + 1, base_xp = ?, base_stress = ? WHERE id = ? AND version = ?", draft.baseXp(), draft.baseStress(), draftId, expectedVersion);
        if (updated != 1) throw new com.josecjuniors.logossrv.core.progressionconfiguration.domain.exception.ProgressionConfigurationAuthoringConflictException("stale draft version");
        jdbc.update("DELETE FROM progression_configuration_draft_factor WHERE draft_id = ?", draftId);
        jdbc.update("DELETE FROM progression_configuration_draft_distribution WHERE draft_id = ?", draftId);
        for (String ignored : draft.factors()) {
            UUID factorId = jdbc.queryForObject("SELECT id FROM fator_calculo WHERE semantic_key = ?", UUID.class, ignored.trim().toLowerCase(java.util.Locale.ROOT));
            jdbc.update("INSERT INTO progression_configuration_draft_factor(id, draft_id, fator_calculo_id) VALUES (?, ?, ?)", UUID.randomUUID(), draftId, factorId);
        }
        for (ProgressionConfigurationDraft.Distribution d : draft.distributions()) {
            UUID distribution = UUID.randomUUID();
            jdbc.update("INSERT INTO progression_configuration_draft_distribution(id, draft_id, attribute_id, weight) VALUES (?, ?, ?, ?)", distribution, draftId, d.attributeId(), d.weight());
            for (ProgressionConfigurationDraft.XpRule rule : d.xpRules()) {
                UUID factor = jdbc.queryForObject("SELECT id FROM fator_calculo WHERE semantic_key = ?", UUID.class, rule.fact().trim().toLowerCase(java.util.Locale.ROOT));
                jdbc.update("INSERT INTO progression_configuration_draft_xp_rule(id, distribution_id, fator_calculo_id, multiplier, min_cutoff, max_cutoff, calculation_mode) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), distribution, factor, rule.multiplier(), rule.minCutoff(), rule.maxCutoff(), rule.calculationMode() == null ? XpCalculationMode.FIXED.name() : rule.calculationMode().name());
            }
            for (ProgressionConfigurationDraft.StressRule rule : d.stressRules()) {
                jdbc.update("INSERT INTO progression_configuration_draft_stress_rule(id, distribution_id, multiplier, min_cutoff, max_cutoff, type) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), distribution, rule.multiplier(), rule.minCutoff(), rule.maxCutoff(), rule.type());
            }
        }
        return findDraft(logicalKey).orElseThrow();
    }

    @Override
    @Transactional
    public Optional<PublishedProgressionConfigurationVersion> findPublishedByDraftVersion(String logicalKey,
                                                                                            long sourceDraftVersion) {
        ProgressionConfigurationDefinition definition = find(logicalKey).orElseThrow();
        UUID definitionId = definition.id();
        jdbc.queryForObject("SELECT id FROM progression_configuration_definition WHERE id = ? FOR UPDATE", UUID.class, definitionId);
        return jdbc.query("SELECT id, revision, source_draft_version FROM progression_configuration_version "
                                + "WHERE definition_id = ? AND source_draft_version = ?",
                        (rs, n) -> new PublishedProgressionConfigurationVersion(definitionId,
                                rs.getObject("id", UUID.class), rs.getInt("revision"), rs.getLong("source_draft_version")),
                        definitionId, sourceDraftVersion).stream().findFirst();
    }

    @Override
    @Transactional
    public Optional<ProgressionConfigurationDraft> lockDraft(String logicalKey, long expectedDraftVersion) {
        ProgressionConfigurationDefinition definition = find(logicalKey).orElseThrow();
        UUID definitionId = definition.id();
        jdbc.queryForObject("SELECT id FROM progression_configuration_definition WHERE id = ? FOR UPDATE", UUID.class, definitionId);
        UUID draftId = jdbc.queryForObject("SELECT id FROM progression_configuration_draft WHERE definition_id = ?", UUID.class, definitionId);
        Long currentVersion = jdbc.queryForObject("SELECT version FROM progression_configuration_draft WHERE id = ? FOR UPDATE",
                Long.class, draftId);
        if (currentVersion == null || currentVersion != expectedDraftVersion) {
            return Optional.empty();
        }
        return findDraft(logicalKey);
    }

    @Override
    @Transactional
    public PublishedProgressionConfigurationVersion publishLocked(String logicalKey, long sourceDraftVersion,
                                                                  ProgressionConfigurationDraft draft) {
        ProgressionConfigurationDefinition definition = find(logicalKey).orElseThrow();
        UUID definitionId = definition.id();
        UUID draftId = jdbc.queryForObject("SELECT id FROM progression_configuration_draft WHERE definition_id = ? AND version = ?",
                UUID.class, definitionId, sourceDraftVersion);
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision), 0) + 1 FROM progression_configuration_version WHERE definition_id = ?",
                Integer.class, definitionId);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO progression_configuration_version(id, definition_id, revision, base_xp, base_stress, fact_key_generation, source_draft_version) VALUES (?, ?, ?, ?, ?, 'SEMANTIC', ?)",
                version, definitionId, revision, draft.baseXp(), draft.baseStress(), sourceDraftVersion);
        for (ProgressionConfigurationDraft.Distribution source : draft.distributions()) {
            UUID distribution = UUID.randomUUID();
            jdbc.update("INSERT INTO progression_configuration_version_distribution(id, configuration_version_id, attribute_key, weight) VALUES (?, ?, ?, ?)",
                    distribution, version, source.attributeId().toString(), source.weight());
            for (ProgressionConfigurationDraft.XpRule rule : source.xpRules()) {
                String fact = rule.fact().trim().toLowerCase(java.util.Locale.ROOT);
                jdbc.update("INSERT INTO progression_configuration_version_xp_rule(id, distribution_id, factor_key, multiplier, min_cutoff, max_cutoff, calculation_mode) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), distribution, fact, rule.multiplier(), rule.minCutoff(), rule.maxCutoff(), rule.calculationMode().name());
            }
            for (ProgressionConfigurationDraft.StressRule rule : source.stressRules()) {
                jdbc.update("INSERT INTO progression_configuration_version_stress_rule(id, distribution_id, multiplier, min_cutoff, max_cutoff, type) VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), distribution, rule.multiplier(), rule.minCutoff(), rule.maxCutoff(), rule.type());
            }
        }
        jdbc.update("INSERT INTO progression_configuration_version_factor(id, configuration_version_id, factor_key, tipo_input) "
                        + "SELECT gen_random_uuid(), ?, f.semantic_key, f.tipo_input FROM fator_calculo f "
                        + "JOIN progression_configuration_draft_factor df ON df.fator_calculo_id = f.id WHERE df.draft_id = ?",
                version, draftId);
        return new PublishedProgressionConfigurationVersion(definitionId, version, revision, sourceDraftVersion);
    }

    @Override
    @Transactional
    public ProgressionConfigurationDefinition activate(String logicalKey, int revision, long expectedActivationVersion) {
        ProgressionConfigurationDefinition current = lockDefinition(logicalKey);
        if (current.legacyLinked()) {
            throw new ProgressionConfigurationActivationConflictException("legacy-linked configuration cannot be activated by modern authoring");
        }
        UUID target = jdbc.query("SELECT v.id FROM progression_configuration_version v JOIN progression_configuration_definition d ON d.id = v.definition_id WHERE d.id = ? AND v.revision = ?",
                (rs, n) -> rs.getObject(1, UUID.class), current.id(), revision).stream().findFirst()
                .orElseThrow(ProgressionConfigurationNotFoundException::new);
        if (current.currentRevision() != null && current.currentRevision() == revision) {
            return current;
        }
        if (current.activationVersion() != expectedActivationVersion) {
            throw new ProgressionConfigurationActivationConflictException("stale activation version");
        }
        int updated = jdbc.update("UPDATE progression_configuration_definition SET current_version_id = ?, activation_version = activation_version + 1 WHERE id = ? AND activation_version = ?",
                target, current.id(), expectedActivationVersion);
        if (updated != 1) {
            throw new ProgressionConfigurationActivationConflictException("stale activation version");
        }
        return find(logicalKey).orElseThrow(ProgressionConfigurationNotFoundException::new);
    }

    @Override
    @Transactional
    public ProgressionConfigurationDefinition deactivate(String logicalKey, long expectedActivationVersion) {
        ProgressionConfigurationDefinition current = lockDefinition(logicalKey);
        if (current.legacyLinked()) {
            throw new ProgressionConfigurationActivationConflictException("legacy-linked configuration cannot be deactivated by modern authoring");
        }
        if (current.currentRevision() == null) {
            return current;
        }
        if (current.activationVersion() != expectedActivationVersion) {
            throw new ProgressionConfigurationActivationConflictException("stale activation version");
        }
        int updated = jdbc.update("UPDATE progression_configuration_definition SET current_version_id = NULL, activation_version = activation_version + 1 WHERE id = ? AND activation_version = ?",
                current.id(), expectedActivationVersion);
        if (updated != 1) {
            throw new ProgressionConfigurationActivationConflictException("stale activation version");
        }
        return find(logicalKey).orElseThrow(ProgressionConfigurationNotFoundException::new);
    }

    private ProgressionConfigurationDefinition lockDefinition(String logicalKey) {
        return jdbc.query("""
                SELECT d.id, d.logical_key, d.legacy_atividade_config_id,
                       v.revision, dr.version, d.activation_version
                FROM progression_configuration_definition d
                LEFT JOIN progression_configuration_version v ON v.id = d.current_version_id
                LEFT JOIN progression_configuration_draft dr ON dr.definition_id = d.id
                WHERE d.logical_key = ?
                FOR UPDATE OF d
                """, (rs, n) -> new ProgressionConfigurationDefinition(
                rs.getObject("id", UUID.class), rs.getString("logical_key"),
                (Integer) rs.getObject("revision"), (Long) rs.getObject("version"),
                rs.getLong("activation_version"),
                rs.getObject("legacy_atividade_config_id") != null), logicalKey).stream().findFirst()
                .orElseThrow(ProgressionConfigurationNotFoundException::new);
    }

    private record DraftRoot(UUID id, long version, Integer baseXp, Integer baseStress) {}
    private record DistributionBuilder(UUID id, UUID attributeId, double weight) {}
}
