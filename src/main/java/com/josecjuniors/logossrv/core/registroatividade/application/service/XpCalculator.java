package com.josecjuniors.logossrv.core.registroatividade.application.service;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMenorOuIgualQue;

/** Encapsula a fórmula de XP já utilizada no processamento de atividades. */
public class XpCalculator {

    public double calculate(ProgressionInput.XpRule regra, double valor, double xpBase, double pesoPercentual) {
        if (nuloOuMenorOuIgualQue(regra.minCutoff(), valor)
                && nuloOuMaiorOuIgualQue(regra.maxCutoff(), valor)) {
            return xpBase * regra.multiplier() * pesoPercentual;
        }
        return (xpBase / regra.multiplier()) * pesoPercentual;
    }
}
