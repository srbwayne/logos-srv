package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;

public record UpdateRegraDistribuicaoCommand(
        RegraDistribuicaoAtividadeId regraId,
        Double pesoPercentual
) {}
