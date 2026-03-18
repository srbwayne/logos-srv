package com.josecjuniors.logossrv.core.jogador.domain.repository;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.jogador.domain.model.DebuffJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.util.Optional;

public interface DebuffJogadorRepository {
    Optional<DebuffJogador> findByJogadorIdAndDebuffId(JogadorId id, DebuffId debuffId);

    DebuffJogador save(DebuffJogador debuffJogador);
}
