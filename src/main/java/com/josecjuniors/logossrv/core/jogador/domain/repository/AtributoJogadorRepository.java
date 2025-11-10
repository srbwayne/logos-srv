package com.josecjuniors.logossrv.core.jogador.domain.repository;

import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AtributoJogadorRepository {
    Page<AtributoJogador> findByJogadorUserEmail(String email, Pageable pageable);
}
