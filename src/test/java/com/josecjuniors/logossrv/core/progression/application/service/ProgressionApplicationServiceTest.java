package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionApplicationServiceTest {

    private final ProgressionApplicationService service = new ProgressionApplicationService();

    @Test
    void executaEngineAplicaResultadoERetornaEstadoAtualizado() {
        var distribution = new ProgressionInput.AttributeDistribution(
                "forca", 1.0,
                List.of(new ProgressionInput.XpRule("peso", 1.0, null, null)),
                List.of(new ProgressionInput.StressRule(2.0, null, null, ProgressionInput.StressType.POSITIVE)));
        var input = new ProgressionInput(
                150, 10,
                List.of(new ProgressionInput.Detail("peso", 100)),
                List.of(distribution), List.of());
        var profile = new ProgressionProfile(
                0, 1, 0, 1,
                List.of(new ProgressionProfile.ProgressionAttribute("forca", 0, 1)), List.of());

        var outcome = service.execute(input, profile);
        var updated = outcome.updatedProfile();

        assertThat(outcome.result().xpGlobal()).isEqualTo(150);
        assertThat(updated.globalXp()).isEqualTo(150);
        assertThat(updated.globalLevel()).isEqualTo(2);
        assertThat(updated.stress()).isEqualTo(20);
        assertThat(updated.skillPoints()).isEqualTo(3);
        assertThat(updated.attributes()).containsExactly(
                new ProgressionProfile.ProgressionAttribute("forca", 150, 2));
    }
}
