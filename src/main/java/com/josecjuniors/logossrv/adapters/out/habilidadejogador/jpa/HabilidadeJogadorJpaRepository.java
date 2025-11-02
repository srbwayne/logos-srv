package com.josecjuniors.logossrv.adapters.out.habilidadejogador.jpa;

import com.josecjuniors.logossrv.core.habilidadejogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.habilidadejogador.domain.model.HabilidadeJogadorId;
import com.josecjuniors.logossrv.core.habilidadejogador.domain.repository.HabilidadeJogadorRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HabilidadeJogadorJpaRepository extends HabilidadeJogadorRepository, JpaRepository<HabilidadeJogador, HabilidadeJogadorId> {}
