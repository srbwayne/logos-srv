package com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record AddAtributoRequest(
        UUID atributoId,
        BigDecimal peso
) {}
