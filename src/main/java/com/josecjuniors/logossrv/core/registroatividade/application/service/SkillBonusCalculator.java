package com.josecjuniors.logossrv.core.registroatividade.application.service;

import java.util.List;
/** Encapsula o bônus de habilidade destinado a um atributo. */
public class SkillBonusCalculator {

    public double calculate(List<ProgressionInput.SkillBonus> bonuses, String attributeKey) {
        return bonuses.stream()
                .filter(bonus -> bonus.attributeKey().equals(attributeKey))
                .mapToDouble(bonus -> bonus.distributionWeight() * bonus.skillLevel() * 0.05)
                .sum();
    }
}
