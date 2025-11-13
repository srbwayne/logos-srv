package com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.request;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;

public record CreateFatorCalculoRequest(
        String nome,
        String unidadeMedida,
        TipoInput tipoInput
) {}
