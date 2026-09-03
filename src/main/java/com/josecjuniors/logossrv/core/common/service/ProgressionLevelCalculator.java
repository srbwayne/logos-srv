package com.josecjuniors.logossrv.core.common.service;

/** Política pura de transição de nível compartilhada pelo perfil e pelo serviço legado. */
public class ProgressionLevelCalculator {

    public LevelResult apply(int level, long storedXp, int stress, long xpGained) {
        if (xpGained <= 0) {
            return new LevelResult(level, 0, false);
        }
        long netXp = (long) (xpGained * stressModifier(stress));
        if (netXp <= 0) {
            return new LevelResult(level, 0, false);
        }

        long xpForNextLevel = xpForNextLevel(level);
        long xpForLevelCheck = storedXp + netXp;
        int resultingLevel = level;
        int levelsGained = 0;
        while (xpForLevelCheck >= xpForNextLevel) {
            xpForLevelCheck -= xpForNextLevel;
            resultingLevel++;
            levelsGained++;
            xpForNextLevel = xpForNextLevel(resultingLevel);
        }
        return new LevelResult(resultingLevel, levelsGained, true);
    }

    public long xpForNextLevel(int level) {
        return (long) (Math.pow(level, 2) * 100) + 50;
    }

    private double stressModifier(int stress) {
        if (stress >= 90) {
            return 0.5;
        } else if (stress >= 60) {
            return 0.7;
        } else if (stress >= 30) {
            return 0.85;
        }
        return 1.0;
    }

    public record LevelResult(int level, int levelsGained, boolean xpApplied) {
    }
}
