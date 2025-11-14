package com.josecjuniors.logossrv.core.atividadeagendada.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.time.LocalDateTime;

public record CreateAtividadeAgendadaCommand(
        JogadorId jogadorId,
        AtividadeConfigId atividadeConfigId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
) {}
