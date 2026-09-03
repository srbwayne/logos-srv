package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.common.service.ProgressionLevelCalculator;

import java.util.ArrayList;
import java.util.List;

/** Estado mínimo de progressão, independente de identidade pessoal e persistência. */
public record ProgressionProfile(
        long globalXp,
        int globalLevel,
        int stress,
        int skillPoints,
        List<ProgressionAttribute> attributes,
        List<SkillState> skills) {

    public ProgressionProfile {
        attributes = List.copyOf(attributes);
        skills = List.copyOf(skills);
    }

    public ProgressionProfile apply(ProgressionResult result) {
        return apply(result, new ProgressionLevelCalculator());
    }

    ProgressionProfile apply(ProgressionResult result, ProgressionLevelCalculator levelCalculator) {
        int resultingStress = Math.max(0, stress + (int) result.stressTotal());
        List<ProgressionAttribute> resultingAttributes = new ArrayList<>(attributes);
        int resultingSkillPoints = skillPoints;
        for (ProgressionResult.AttributeProgression progression : result.attributeProgressions()) {
            int index = findAttribute(resultingAttributes, progression.attributeKey());
            ProgressionAttribute current = index < 0
                    ? new ProgressionAttribute(progression.attributeKey(), 0, 1)
                    : resultingAttributes.get(index);
            ProgressionLevelCalculator.LevelResult levelResult = levelCalculator.apply(
                    current.level(), current.xp(), resultingStress, progression.xp());
            if (levelResult.xpApplied()) {
                current = new ProgressionAttribute(
                        current.key(), current.xp() + progression.xp(), levelResult.level());
                resultingSkillPoints += levelResult.levelsGained();
            }
            if (index < 0) {
                resultingAttributes.add(current);
            } else {
                resultingAttributes.set(index, current);
            }
        }

        ProgressionLevelCalculator.LevelResult globalResult = levelCalculator.apply(
                globalLevel, globalXp, resultingStress, result.xpGlobal());
        if (globalResult.xpApplied()) {
            resultingSkillPoints += globalResult.levelsGained();
        }
        return new ProgressionProfile(
                globalResult.xpApplied() ? globalXp + result.xpGlobal() : globalXp,
                globalResult.level(),
                resultingStress,
                resultingSkillPoints,
                resultingAttributes,
                skills);
    }

    private int findAttribute(List<ProgressionAttribute> values, String key) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).key().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    public record ProgressionAttribute(String key, long xp, int level) {
    }

    public record SkillState(String key, int level) {
    }
}
