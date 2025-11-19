package com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateRegistroAtividadeRequest(
        UUID atividadeConfigId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        List<DetalheRegistroRequest> detalhes
) {
}
