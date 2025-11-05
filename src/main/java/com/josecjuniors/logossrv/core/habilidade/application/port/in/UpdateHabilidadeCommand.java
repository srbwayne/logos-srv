package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;

public record UpdateHabilidadeCommand(
        HabilidadeId habilidadeId,
        String novoNome,
        String novaDescricao
) {}
