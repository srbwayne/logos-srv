package com.josecjuniors.logossrv.core.registroatividade.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionProfileTest {

    @Test
    void aplicaXpGlobalEstresseAtributosENovosPontosSemEntidadesPersistentes() {
        var profile = new ProgressionProfile(
                0, 1, 0, 1,
                List.of(
                        new ProgressionProfile.ProgressionAttribute("forca", 0, 1),
                        new ProgressionProfile.ProgressionAttribute("foco", 0, 1)),
                List.of(new ProgressionProfile.SkillState("espada", 1)));

        var updated = profile.apply(new ProgressionResult(
                150, 0, List.of(
                        new ProgressionResult.AttributeProgression("forca", 150),
                        new ProgressionResult.AttributeProgression("foco", 10))));

        assertThat(updated.globalXp()).isEqualTo(150);
        assertThat(updated.globalLevel()).isEqualTo(2);
        assertThat(updated.skillPoints()).isEqualTo(3);
        assertThat(updated.attributes()).containsExactly(
                new ProgressionProfile.ProgressionAttribute("forca", 150, 2),
                new ProgressionProfile.ProgressionAttribute("foco", 10, 1));
        assertThat(updated.skills()).containsExactly(new ProgressionProfile.SkillState("espada", 1));
    }

    @Test
    void aplicaDeltaDeEstresseComPisoZeroMesmoComValorNegativo() {
        var profile = new ProgressionProfile(20, 1, 15, 0, List.of(), List.of());

        var updated = profile.apply(new ProgressionResult(0, -30, List.of()));

        assertThat(updated.stress()).isZero();
        assertThat(updated.globalXp()).isEqualTo(20);
        assertThat(updated.globalLevel()).isEqualTo(1);
        assertThat(updated.skillPoints()).isZero();
    }
}
