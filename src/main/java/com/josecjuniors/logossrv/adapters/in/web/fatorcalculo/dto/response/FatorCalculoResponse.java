package com.josecjuniors.logossrv.adapters.in.web.fatorcalculo.dto.response;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;

public record FatorCalculoResponse(
        String id,
        String nome,
        String unidadeMedida,
        TipoInput tipoInput
) {
    public static FatorCalculoResponse fromDto(FatorCalculoDto dto) {
        return new FatorCalculoResponse(dto.id(), dto.nome(), dto.unidadeMedida(), dto.tipoInput());
    }
}
