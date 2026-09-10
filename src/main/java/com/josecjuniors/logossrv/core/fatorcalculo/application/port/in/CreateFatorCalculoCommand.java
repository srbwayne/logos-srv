package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;

public record CreateFatorCalculoCommand(
        String semanticKey,
        String nome,
        String unidadeMedida,
        TipoInput tipoInput
) {}
