package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command;

import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;

public record CreateRegraDistribuicaoHabilidadeCommand(
        HabilidadeId habilidadeId,
        AtributoId atributoId,
        Double pesoDistribuicao
) {}
