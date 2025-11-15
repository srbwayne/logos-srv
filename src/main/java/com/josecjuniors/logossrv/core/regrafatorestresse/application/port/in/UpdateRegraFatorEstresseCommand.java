package com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;

public record UpdateRegraFatorEstresseCommand(
        RegraFatorEstresseId regraFatorEstresseId,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax,
        TipoFatorEstresse tipo
) {}
