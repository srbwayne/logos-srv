package com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.response;

import com.josecjuniors.logossrv.core.regrafatorestresse.application.dto.RegraFatorEstresseDto;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record RegraFatorEstresseResponse(
        String id,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {
    public static RegraFatorEstresseResponse fromDto(RegraFatorEstresseDto dto) {
        return new RegraFatorEstresseResponse(
                dto.id(),
                dto.pesoMultiplicador(),
                dto.pontoCorteMin(),
                dto.pontoCorteMax(),
                dto.tipo()
        );
    }
}
