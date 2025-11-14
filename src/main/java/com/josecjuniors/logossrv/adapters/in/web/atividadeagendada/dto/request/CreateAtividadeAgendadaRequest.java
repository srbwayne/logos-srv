package com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAtividadeAgendadaRequest(
        UUID atividadeConfigId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
) {}
