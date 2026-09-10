package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.response;

import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;

import java.util.List;
import java.util.UUID;

public record ProgressionConfigurationDraftResponse(
        UUID definitionId,
        long version,
        Integer baseXp,
        Integer baseStress,
        List<String> factors,
        List<DistributionResponse> distributions) {
    public static ProgressionConfigurationDraftResponse from(ProgressionConfigurationDraft value) {
        return new ProgressionConfigurationDraftResponse(value.definitionId(), value.version(), value.baseXp(), value.baseStress(), value.factors(), value.distributions().stream().map(DistributionResponse::from).toList());
    }

    public record DistributionResponse(UUID attributeId, double weight, List<ProgressionConfigurationDraft.XpRule> xpRules, List<ProgressionConfigurationDraft.StressRule> stressRules) {
        static DistributionResponse from(ProgressionConfigurationDraft.Distribution value) {
            return new DistributionResponse(value.attributeId(), value.weight(), value.xpRules(), value.stressRules());
        }
    }
}
