package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;

public record UpdateRegraDistribuicaoHabilidadeCommand(
        HabilidadeId habilidadeId,
        RegraDistribuicaoHabilidadeId regraId,
        Double pesoDistribuicao
) {}
