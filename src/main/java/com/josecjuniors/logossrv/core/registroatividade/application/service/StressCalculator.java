package com.josecjuniors.logossrv.core.registroatividade.application.service;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMenorOuIgualQue;

/** Encapsula a fórmula de estresse já utilizada no processamento de atividades. */
public class StressCalculator {

    public double calculate(ProgressionInput.StressRule regra, double valor, double estresseBase) {
        double estresseCalculado = estresseBase;
        if (naoNuloEMenorOuIgualQue(regra.minCutoff(), valor)) {
            estresseCalculado = estresseBase / regra.multiplier();
        } else if (naoNuloEMaiorOuIgualQue(regra.maxCutoff(), valor)) {
            estresseCalculado = estresseBase * regra.multiplier();
        }
        return regra.type() == ProgressionInput.StressType.NEGATIVE ? -estresseCalculado : estresseCalculado;
    }
}
