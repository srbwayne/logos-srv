package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.request;

import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;

import java.util.List;
import java.util.UUID;

public record UpdateProgressionConfigurationDraftRequest(
        long expectedVersion,
        Integer baseXp,
        Integer baseStress,
        List<String> factors,
        List<DistributionRequest> distributions) {
    public record DistributionRequest(
            UUID attributeId,
            double weight,
            List<XpRuleRequest> xpRules,
            List<StressRuleRequest> stressRules) {}

    public record XpRuleRequest(
            String fact,
            Double multiplier,
            Double minCutoff,
            Double maxCutoff,
            XpCalculationMode calculationMode) {}

    public record StressRuleRequest(
            Double multiplier,
            Double minCutoff,
            Double maxCutoff,
            String type) {}
}
