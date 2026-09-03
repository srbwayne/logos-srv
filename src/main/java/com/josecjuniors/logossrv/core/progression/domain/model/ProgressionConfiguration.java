package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.List;

/** Regras resolvidas pelo Logos para uma referência de configuração. */
public record ProgressionConfiguration(
        int baseXp,
        int baseStress,
        List<AttributeDistribution> attributeDistributions,
        List<SkillBonusRule> skillBonusRules) {

    public ProgressionConfiguration {
        attributeDistributions = List.copyOf(attributeDistributions);
        skillBonusRules = List.copyOf(skillBonusRules);
    }

    public record AttributeDistribution(
            String attributeKey,
            double weight,
            List<XpRule> xpRules,
            List<StressRule> stressRules) {
        public AttributeDistribution {
            xpRules = List.copyOf(xpRules);
            stressRules = List.copyOf(stressRules);
        }
    }

    public record XpRule(String factorKey, double multiplier, Double minCutoff, Double maxCutoff) {
    }

    public record StressRule(double multiplier, Double minCutoff, Double maxCutoff, StressType type) {
    }

    public record SkillBonusRule(String skillKey, String attributeKey, double distributionWeight) {
    }

    public enum StressType { POSITIVE, NEGATIVE }
}
