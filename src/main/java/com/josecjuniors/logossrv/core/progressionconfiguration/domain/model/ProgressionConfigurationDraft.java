package com.josecjuniors.logossrv.core.progressionconfiguration.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;

import java.util.List;
import java.util.UUID;

public record ProgressionConfigurationDraft(
        UUID definitionId,
        long version,
        Integer baseXp,
        Integer baseStress,
        List<String> factors,
        List<Distribution> distributions) {
    public ProgressionConfigurationDraft {
        factors = List.copyOf(factors);
        distributions = List.copyOf(distributions);
    }

    public record Distribution(
            UUID attributeId,
            double weight,
            List<XpRule> xpRules,
            List<StressRule> stressRules) {
        public Distribution {
            xpRules = List.copyOf(xpRules);
            stressRules = List.copyOf(stressRules);
        }
    }

    public record XpRule(
            String fact,
            Double multiplier,
            Double minCutoff,
            Double maxCutoff,
            XpCalculationMode calculationMode) {
    }

    public record StressRule(
            Double multiplier,
            Double minCutoff,
            Double maxCutoff,
            String type) {
    }
}
