package com.josecjuniors.logossrv.core.registroatividade.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateRegistroAtividadeCommand(
        String userEmail, // Alterado de JogadorId para userEmail
        UUID atividadeConfigId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer formVersion,
        List<RegistroAtividadeDetalheCommand> detalhes
) {}
