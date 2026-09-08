package com.josecjuniors.logossrv.adapters.in.web.vicio.dto.request;

import java.util.UUID;

public record CreateRegraVicioRequest(
        Integer impactoEstresse,
        Integer penalidadePontos,
        Integer duracaoHoras,
        Integer xpGanhoRecaida,
        UUID debuffId
) {}
