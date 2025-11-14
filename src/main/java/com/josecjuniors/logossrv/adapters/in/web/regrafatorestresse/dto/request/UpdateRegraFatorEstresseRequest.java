package com.josecjuniors.logossrv.adapters.in.web.regrafatorestresse.dto.request;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record UpdateRegraFatorEstresseRequest(
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {}
