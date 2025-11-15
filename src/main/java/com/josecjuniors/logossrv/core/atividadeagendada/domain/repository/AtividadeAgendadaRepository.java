package com.josecjuniors.logossrv.core.atividadeagendada.domain.repository;

import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AtividadeAgendadaRepository {
    AtividadeAgendada save(AtividadeAgendada agendamento);
    Optional<AtividadeAgendada> findById(AtividadeAgendadaId id);
    List<AtividadeAgendada> findByJogadorIdAndPeriodo(JogadorId jogadorId, LocalDateTime inicio, LocalDateTime fim);
    void deleteAll();
}
