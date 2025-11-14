package com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record CreateRegraFatorEstresseCommand(
        RegraDistribuicaoAtividadeId regraDistribuicaoId,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {}
