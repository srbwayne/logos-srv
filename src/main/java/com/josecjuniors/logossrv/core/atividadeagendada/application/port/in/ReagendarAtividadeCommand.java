package com.josecjuniors.logossrv.core.atividadeagendada.application.port.in;

import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.time.LocalDateTime;

public record ReagendarAtividadeCommand(
        JogadorId jogadorId,
        AtividadeAgendadaId atividadeAgendadaId,
        LocalDateTime novaDataHoraInicio,
        LocalDateTime novaDataHoraFim
) {}
