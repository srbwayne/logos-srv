package com.josecjuniors.logossrv.core.regrafatorxp.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;

public record UpdateRegraFatorXPCommand(
        RegraFatorXPId regraFatorXPId,
        FatorCalculoId fatorCalculoId,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {}
