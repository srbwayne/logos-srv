package com.josecjuniors.logossrv.adapters.in.web.progression;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionEvaluationResponse;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionEvaluationResponseTest {
    @Test
    void legacyOutcomeExposesNullableSemanticKeys() {
        String key = "6da84f5a-be26-4d78-89d6-03af5530160a";
        var outcome = new ProgressionOutcome(
                new ProgressionResult(30, 0, List.of(new ProgressionResult.AttributeProgression(key, 30))),
                new ProgressionProfile(30, 1, 0, 0,
                        List.of(new ProgressionProfile.ProgressionAttribute(key, 30, 1)), List.of()));

        var response = ProgressionEvaluationResponse.from(outcome);

        assertThat(response.result().attributeProgressions().get(0).key()).isEqualTo(key);
        assertThat(response.result().attributeProgressions().get(0).semanticKey()).isNull();
        assertThat(response.profile().attributes().get(0).semanticKey()).isNull();
    }
}
