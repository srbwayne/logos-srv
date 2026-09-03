package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionInputFactoryTest {

    @Test
    void combinaFatosConfiguracaoEEstadoDeSkillSemExporRegrasAoFato() {
        var configuration = new ProgressionConfiguration(
                100, 5,
                List.of(new ProgressionConfiguration.AttributeDistribution(
                        "forca", .5,
                        List.of(new ProgressionConfiguration.XpRule("peso", 2, null, 120.0)),
                        List.of(new ProgressionConfiguration.StressRule(1.5, null, null,
                                ProgressionConfiguration.StressType.POSITIVE)))),
                List.of(new ProgressionConfiguration.SkillBonusRule("espada", "forca", 10)));
        var fact = new ProgressionFact(List.of(new ProgressionFact.Detail("peso", 100)));
        var profile = new ProgressionProfile(0, 1, 0, 1, List.of(),
                List.of(new ProgressionProfile.SkillState("espada", 3)));

        var input = new ProgressionInputFactory().create(fact, configuration, profile);

        assertThat(input.baseXp()).isEqualTo(100);
        assertThat(input.baseStress()).isEqualTo(5);
        assertThat(input.details()).containsExactly(new com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput.Detail("peso", 100));
        assertThat(input.attributeDistributions()).hasSize(1);
        assertThat(input.attributeDistributions().get(0).xpRules()).hasSize(1);
        assertThat(input.skillBonuses()).containsExactly(
                new com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput.SkillBonus("forca", 10, 3));
    }
}
