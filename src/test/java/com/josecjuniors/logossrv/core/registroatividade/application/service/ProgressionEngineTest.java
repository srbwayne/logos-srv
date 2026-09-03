package com.josecjuniors.logossrv.core.registroatividade.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionEngineTest {

    private final ProgressionEngine engine = new ProgressionEngine();

    @Test
    void calculaProgressaoDiretamenteComInputSemSpringOuPersistencia() {
        var forca = new ProgressionInput.AttributeDistribution(
                "forca", .5,
                List.of(new ProgressionInput.XpRule("peso", 1.5, 80.0, 120.0)),
                List.of(new ProgressionInput.StressRule(2.0, 100.0, null, ProgressionInput.StressType.POSITIVE)));
        var foco = new ProgressionInput.AttributeDistribution(
                "foco", .5,
                List.of(new ProgressionInput.XpRule("repeticoes", 2.0, null, null)),
                List.of(new ProgressionInput.StressRule(2.0, null, 20.0, ProgressionInput.StressType.NEGATIVE)));
        var input = new ProgressionInput(
                100,
                10,
                List.of(new ProgressionInput.Detail("peso", 100), new ProgressionInput.Detail("repeticoes", 10)),
                List.of(forca, foco),
                List.of(new ProgressionInput.SkillBonus("forca", 10, 3)));

        ProgressionResult result = engine.calculate(input);

        assertThat(result.xpGlobal()).isEqualTo(175);
        assertThat(result.stressTotal()).isEqualTo(-5.0);
        assertThat(result.attributeProgressions())
                .extracting(ProgressionResult.AttributeProgression::attributeKey,
                        ProgressionResult.AttributeProgression::xp)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("forca", 76L),
                        org.assertj.core.groups.Tuple.tuple("foco", 100L));
    }

    @Test
    void preservaTruncamentoDeXpCalculadoNoResultado() {
        var distribution = new ProgressionInput.AttributeDistribution(
                "forca", .25,
                List.of(new ProgressionInput.XpRule("peso", 1.5, null, null)),
                List.of());

        ProgressionResult result = engine.calculate(new ProgressionInput(
                101, 0, List.of(new ProgressionInput.Detail("peso", 999)), List.of(distribution), List.of()));

        assertThat(result.xpGlobal()).isEqualTo(37);
        assertThat(result.attributeProgressions()).singleElement()
                .extracting(ProgressionResult.AttributeProgression::xp).isEqualTo(37L);
    }
}
