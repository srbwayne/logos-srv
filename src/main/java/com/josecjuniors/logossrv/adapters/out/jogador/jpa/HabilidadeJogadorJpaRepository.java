package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.HabilidadeJogadorRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HabilidadeJogadorJpaRepository extends HabilidadeJogadorRepository, JpaRepository<HabilidadeJogador, HabilidadeJogadorId> {}
