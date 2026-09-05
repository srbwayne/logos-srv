package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Motor determinístico de progressão; recebe somente o contrato de cálculo interno. */
public class ProgressionEngine {

    private final XpCalculator xpCalculator;
    private final StressCalculator stressCalculator;
    private final SkillBonusCalculator skillBonusCalculator;

    public ProgressionEngine() {
        this(new XpCalculator(), new StressCalculator(), new SkillBonusCalculator());
    }

    ProgressionEngine(XpCalculator xpCalculator, StressCalculator stressCalculator, SkillBonusCalculator skillBonusCalculator) {
        this.xpCalculator = xpCalculator;
        this.stressCalculator = stressCalculator;
        this.skillBonusCalculator = skillBonusCalculator;
    }

    public ProgressionResult calculate(ProgressionInput input) {
        validateFactValueRules(input);
        Map<ProgressionInput.AttributeDistribution, Double> xpPorDistribuicao = new HashMap<>();
        double stressTotal = input.baseStress();

        for (ProgressionInput.Detail detail : input.details()) {
            for (ProgressionInput.AttributeDistribution distribution : input.attributeDistributions()) {
                for (ProgressionInput.XpRule xpRule : distribution.xpRules()) {
                    if (xpRule.factorKey().equals(detail.factorKey())) {
                        double xp = xpCalculator.calculate(xpRule, detail.value(), input.baseXp(), distribution.weight());
                        xpPorDistribuicao.merge(distribution, xp, Double::sum);
                    }
                }
                for (ProgressionInput.StressRule stressRule : distribution.stressRules()) {
                    stressTotal += stressCalculator.calculate(stressRule, detail.value(), input.baseStress());
                }
            }
        }

        long xpGlobal = xpPorDistribuicao.isEmpty()
                ? input.baseXp()
                : xpPorDistribuicao.values().stream().mapToLong(Double::longValue).sum();
        List<ProgressionResult.AttributeProgression> attributeProgressions = new ArrayList<>();
        xpPorDistribuicao.forEach((distribution, xp) -> {
            double bonus = skillBonusCalculator.calculate(input.skillBonuses(), distribution.attributeKey());
            attributeProgressions.add(new ProgressionResult.AttributeProgression(
                    distribution.attributeKey(), xp.longValue() + (long) bonus));
        });
        return new ProgressionResult(xpGlobal, stressTotal, attributeProgressions);
    }

    private void validateFactValueRules(ProgressionInput input) {
        Set<String> factKeys = input.details().stream()
                .map(ProgressionInput.Detail::factorKey)
                .collect(java.util.stream.Collectors.toSet());
        for (ProgressionInput.AttributeDistribution distribution : input.attributeDistributions()) {
            for (ProgressionInput.XpRule rule : distribution.xpRules()) {
                if (rule.calculationMode() != XpCalculationMode.FACT_VALUE) continue;
                if (!factKeys.contains(rule.factorKey())) {
                    throw new IllegalArgumentException("Fato obrigatorio ausente para regra FACT_VALUE: " + rule.factorKey());
                }
                input.details().stream()
                        .filter(detail -> detail.factorKey().equals(rule.factorKey()))
                        .filter(detail -> detail.value() < 0)
                        .findFirst()
                        .ifPresent(detail -> {
                            throw new IllegalArgumentException("Fato FACT_VALUE nao pode ser negativo: " + rule.factorKey());
                        });
            }
        }
    }
}
