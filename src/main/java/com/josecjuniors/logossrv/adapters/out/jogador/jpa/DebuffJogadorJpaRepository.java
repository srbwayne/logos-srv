package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.jogador.domain.model.DebuffJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.DebuffJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.DebuffJogadorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DebuffJogadorJpaRepository extends DebuffJogadorRepository, JpaRepository<DebuffJogador, DebuffJogadorId> {

    @Override
    Optional<DebuffJogador> findByJogadorIdAndDebuffId(JogadorId id, DebuffId debuffId);

    @Override
    DebuffJogador save(DebuffJogador debuffJogador);

}
