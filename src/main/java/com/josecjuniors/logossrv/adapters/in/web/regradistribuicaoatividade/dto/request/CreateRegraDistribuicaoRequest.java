package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.request;

import java.util.UUID;

public record CreateRegraDistribuicaoRequest(
        UUID atributoId,
        Double pesoPercentual
) {}
