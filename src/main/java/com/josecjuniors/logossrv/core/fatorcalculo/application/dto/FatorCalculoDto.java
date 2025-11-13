package com.josecjuniors.logossrv.core.fatorcalculo.application.dto;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;

public record FatorCalculoDto(
        String id,
        String nome,
        String unidadeMedida,
        TipoInput tipoInput
) {
    public static FatorCalculoDto fromDomain(FatorCalculo fatorCalculo) {
        return new FatorCalculoDto(
                fatorCalculo.getId().getValue().toString(),
                fatorCalculo.getNome(),
                fatorCalculo.getUnidadeMedida(),
                fatorCalculo.getTipoInput()
        );
    }
}
