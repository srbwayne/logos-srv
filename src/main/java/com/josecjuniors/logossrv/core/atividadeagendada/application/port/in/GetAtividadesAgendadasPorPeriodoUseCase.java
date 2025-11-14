package com.josecjuniors.logossrv.core.atividadeagendada.application.port.in;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.time.LocalDateTime;
import java.util.List;

public interface GetAtividadesAgendadasPorPeriodoUseCase {
    List<AtividadeAgendadaDto> getByPeriodo(JogadorId jogadorId, LocalDateTime inicio, LocalDateTime fim);
}
