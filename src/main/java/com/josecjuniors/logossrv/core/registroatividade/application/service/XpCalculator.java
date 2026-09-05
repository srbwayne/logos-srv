package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.XpCalculationMode;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMenorOuIgualQue;

/** Encapsula a fórmula de XP já utilizada no processamento de atividades. */
public class XpCalculator {

    public double calculate(ProgressionInput.XpRule regra, double valor, double xpBase, double pesoPercentual) {
        if (nuloOuMenorOuIgualQue(regra.minCutoff(), valor)
                && nuloOuMaiorOuIgualQue(regra.maxCutoff(), valor)) {
            return calculate(regra.calculationMode(), xpBase * regra.multiplier() * pesoPercentual, valor);
        }
        return calculate(regra.calculationMode(), (xpBase / regra.multiplier()) * pesoPercentual, valor);
    }

    private double calculate(XpCalculationMode mode, double fixedXp, double factValue) {
        return mode == XpCalculationMode.FACT_VALUE ? fixedXp * factValue : fixedXp;
    }
}
