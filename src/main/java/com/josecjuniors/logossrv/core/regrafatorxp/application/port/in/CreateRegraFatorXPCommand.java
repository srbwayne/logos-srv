package com.josecjuniors.logossrv.core.regrafatorxp.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;

public record CreateRegraFatorXPCommand(
        RegraDistribuicaoAtividadeId regraDistribuicaoId,
        FatorCalculoId fatorCalculoId,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {}
