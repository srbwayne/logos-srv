package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;

import java.util.List;

/** Combina fatos, configuração e estado de skills no input interno do engine. */
public class ProgressionInputFactory {

    public ProgressionInput create(ProgressionFact fact, ProgressionConfiguration configuration,
                                   ProgressionProfile profile) {
        List<ProgressionInput.Detail> details = fact.details().stream()
                .map(detail -> new ProgressionInput.Detail(detail.factorKey(), detail.value()))
                .toList();
        List<ProgressionInput.AttributeDistribution> distributions = configuration.attributeDistributions().stream()
                .map(this::toDistribution)
                .toList();
        List<ProgressionInput.SkillBonus> bonuses = configuration.skillBonusRules().stream()
                .flatMap(rule -> profile.skills().stream()
                        .filter(skill -> skill.key().equals(rule.skillKey()))
                        .map(skill -> new ProgressionInput.SkillBonus(
                                rule.attributeKey(), rule.distributionWeight(), skill.level())))
                .toList();
        return new ProgressionInput(configuration.baseXp(), configuration.baseStress(), details, distributions, bonuses);
    }

    private ProgressionInput.AttributeDistribution toDistribution(ProgressionConfiguration.AttributeDistribution distribution) {
        return new ProgressionInput.AttributeDistribution(
                distribution.attributeKey(), distribution.weight(),
                distribution.xpRules().stream().map(rule -> new ProgressionInput.XpRule(
                        rule.factorKey(), rule.multiplier(), rule.minCutoff(), rule.maxCutoff())).toList(),
                distribution.stressRules().stream().map(rule -> new ProgressionInput.StressRule(
                        rule.multiplier(), rule.minCutoff(), rule.maxCutoff(),
                        rule.type() == ProgressionConfiguration.StressType.NEGATIVE
                                ? ProgressionInput.StressType.NEGATIVE : ProgressionInput.StressType.POSITIVE)).toList());
    }
}
