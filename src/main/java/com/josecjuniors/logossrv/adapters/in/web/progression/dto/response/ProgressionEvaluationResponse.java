package com.josecjuniors.logossrv.adapters.in.web.progression.dto.response;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;

import java.util.List;

public record ProgressionEvaluationResponse(ResultResponse result, ProfileResponse profile) {

    public static ProgressionEvaluationResponse from(ProgressionOutcome outcome) {
        ProgressionResult result = outcome.result();
        ProgressionProfile profile = outcome.updatedProfile();
        return new ProgressionEvaluationResponse(
                new ResultResponse(result.xpGlobal(), result.stressTotal(), result.attributeProgressions().stream()
                        .map(value -> new AttributeProgressionResponse(value.attributeKey(), value.xp())).toList()),
                new ProfileResponse(profile.globalXp(), profile.globalLevel(), profile.stress(), profile.skillPoints(),
                        profile.attributes().stream().map(value -> new AttributeStateResponse(value.key(), value.xp(), value.level())).toList(),
                        profile.skills().stream().map(value -> new SkillResponse(value.key(), value.level())).toList()));
    }

    public record ResultResponse(long globalXpDelta, double stressTotal,
                                 List<AttributeProgressionResponse> attributeProgressions) {
    }

    public record ProfileResponse(long globalXp, int globalLevel, int stress, int skillPoints,
                                  List<AttributeStateResponse> attributes, List<SkillResponse> skills) {
    }

    public record AttributeProgressionResponse(String key, long xp) {
    }

    public record AttributeStateResponse(String key, long xp, int level) {
    }

    public record SkillResponse(String key, int level) {
    }
}
