package com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.request;

import java.util.UUID;

public record CreateRegraFatorXPRequest(
        UUID fatorCalculoId,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {}
