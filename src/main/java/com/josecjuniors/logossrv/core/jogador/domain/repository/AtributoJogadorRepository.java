package com.josecjuniors.logossrv.core.jogador.domain.repository;

import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface AtributoJogadorRepository {
    Page<AtributoJogador> findByJogadorUserEmail(String email, Pageable pageable);

    Set<AtributoJogador> findByJogadorId(JogadorId jogadorId);
}
