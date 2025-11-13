package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;

public record CreateRegraDistribuicaoCommand(
        AtividadeConfigId atividadeConfigId,
        AtributoId atributoId,
        Double pesoPercentual
) {}
