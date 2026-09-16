package com.josecjuniors.logossrv.core.registroatividade.application.port.in;

import java.util.UUID;

public record RegistroAtividadeDetalheCommand(
        UUID fatorCalculoId,
        String valor
) {}
