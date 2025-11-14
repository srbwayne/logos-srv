package com.josecjuniors.logossrv.core.regrafatorestresse.application.dto;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record RegraFatorEstresseDto(
        String id,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {
    public static RegraFatorEstresseDto fromDomain(RegraFatorEstresse regra) {
        return new RegraFatorEstresseDto(
                regra.getId().getValue().toString(),
                regra.getPesoMultiplicador(),
                regra.getPontoCorteMin(),
                regra.getPontoCorteMax(),
                regra.getTipo()
        );
    }
}
