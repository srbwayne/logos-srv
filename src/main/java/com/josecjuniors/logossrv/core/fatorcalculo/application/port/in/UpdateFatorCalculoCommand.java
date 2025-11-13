package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;

public record UpdateFatorCalculoCommand(
        FatorCalculoId fatorCalculoId,
        String nome,
        String unidadeMedida,
        TipoInput tipoInput
) {}
