package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMenorOuIgualQue;

/** Encapsula a fórmula de XP já utilizada no processamento de atividades. */
public class XpCalculator {

    public double calculate(RegraFatorXP regra, double valor, double xpBase, Double pesoPercentual) {
        if (nuloOuMenorOuIgualQue(regra.getPontoCorteMin(), valor)
                && nuloOuMaiorOuIgualQue(regra.getPontoCorteMax(), valor)) {
            return xpBase * regra.getPesoMultiplicador() * pesoPercentual;
        }
        return (xpBase / regra.getPesoMultiplicador()) * pesoPercentual;
    }
}
