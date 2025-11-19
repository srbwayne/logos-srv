package com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request;

import java.util.UUID;

public record DetalheRegistroRequest(
        UUID fatorCalculoId,
        String valor
) {}
