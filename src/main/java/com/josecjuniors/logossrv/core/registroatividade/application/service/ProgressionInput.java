package com.josecjuniors.logossrv.core.registroatividade.application.service;

import java.util.List;

/** Entrada imutável do cálculo de progressão, sem entidades ou dependências de infraestrutura. */
public record ProgressionInput(
        int baseXp,
        int baseStress,
        List<Detail> details,
        List<AttributeDistribution> attributeDistributions,
        List<SkillBonus> skillBonuses) {

    public ProgressionInput {
        details = List.copyOf(details);
        attributeDistributions = List.copyOf(attributeDistributions);
        skillBonuses = List.copyOf(skillBonuses);
    }

    public record Detail(String factorKey, double value) {}

    public record AttributeDistribution(String attributeKey, double weight, List<XpRule> xpRules, List<StressRule> stressRules) {
        public AttributeDistribution {
            xpRules = List.copyOf(xpRules);
            stressRules = List.copyOf(stressRules);
        }
    }

    public record XpRule(String factorKey, double multiplier, Double minCutoff, Double maxCutoff) {}

    public record StressRule(double multiplier, Double minCutoff, Double maxCutoff, StressType type) {}

    public record SkillBonus(String attributeKey, double distributionWeight, int skillLevel) {}

    public enum StressType { POSITIVE, NEGATIVE }
}
