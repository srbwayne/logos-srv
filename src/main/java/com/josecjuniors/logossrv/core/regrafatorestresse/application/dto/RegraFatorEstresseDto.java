package com.josecjuniors.logossrv.core.regrafatorestresse.application.dto;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record RegraFatorEstresseDto(
        String id,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {
}
