package com.josecjuniors.logossrv.core.atributo.application.port.in;

import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;

public record UpdateAtributoCommand(
        AtributoId atributoId,
        String novoNome,
        String novaDescricao
) {}
