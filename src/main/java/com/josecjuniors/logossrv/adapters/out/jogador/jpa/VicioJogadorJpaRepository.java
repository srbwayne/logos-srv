package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.VicioJogadorRepository;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VicioJogadorJpaRepository extends VicioJogadorRepository, JpaRepository<VicioJogador, VicioJogadorId> {

    @Override
    Optional<VicioJogador> findByJogadorIdAndVicioId(JogadorId jogadorId, VicioId vicioId);

    @Override
    void deleteAll();

    @Override
    VicioJogador save(VicioJogador vicioJogador);

    @Override
    long countByJogador_Id(JogadorId jogadorId);

}
