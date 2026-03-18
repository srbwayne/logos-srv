package com.josecjuniors.logossrv.adapters.in.web.registrovicio.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateRegistroVicioRequest(
        UUID vicioId, // O jogador informa o vício, não a "luta"
        LocalDateTime dataHora,
        String observacao
) {}
