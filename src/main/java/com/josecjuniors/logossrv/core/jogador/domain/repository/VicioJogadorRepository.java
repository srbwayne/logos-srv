package com.josecjuniors.logossrv.core.jogador.domain.repository;

import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

import java.util.Optional;

public interface VicioJogadorRepository {
    Optional<VicioJogador> findByJogadorIdAndVicioId(JogadorId jogadorId, VicioId vicioId);

    void deleteAll();

    VicioJogador save(VicioJogador vicioJogador);

    long countByJogador_Id(JogadorId jogadorId);

}
