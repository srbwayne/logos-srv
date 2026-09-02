package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMenorOuIgualQue;

/** Encapsula a fórmula de estresse já utilizada no processamento de atividades. */
public class StressCalculator {

    public double calculate(RegraFatorEstresse regra, double valor, double estresseBase) {
        double estresseCalculado = estresseBase;
        if (naoNuloEMenorOuIgualQue(regra.getPontoCorteMin(), valor)) {
            estresseCalculado = estresseBase / regra.getPesoMultiplicador();
        } else if (naoNuloEMaiorOuIgualQue(regra.getPontoCorteMax(), valor)) {
            estresseCalculado = estresseBase * regra.getPesoMultiplicador();
        }
        return regra.getTipo() == TipoFatorEstresse.NEGATIVO ? -estresseCalculado : estresseCalculado;
    }
}
