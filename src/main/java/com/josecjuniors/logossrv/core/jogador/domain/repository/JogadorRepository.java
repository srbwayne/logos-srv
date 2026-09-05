package com.josecjuniors.logossrv.core.jogador.domain.repository;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;

import java.util.Optional;

public interface JogadorRepository {
    Jogador save(Jogador jogador);
    Optional<Jogador> findById(JogadorId id);
    Optional<Jogador> findByIdForUpdate(JogadorId id);
    Optional<Jogador> findByAppUserId(AppUserId appUserId);
    Optional<Jogador> findByAppUserIdForUpdate(AppUserId appUserId);
    Optional<Jogador> findByUserEmail(String email);
    boolean existsByApelidoAndIdNot(String apelido, JogadorId id);
    void deleteAll();
}
