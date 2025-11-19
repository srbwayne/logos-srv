package com.josecjuniors.logossrv.core.registroatividade.application.port.in;

import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.DetalheRegistroRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateRegistroAtividadeCommand(
        String userEmail, // Alterado de JogadorId para userEmail
        UUID atividadeConfigId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        List<DetalheRegistroRequest> detalhes
) {}
