package com.josecjuniors.logossrv.adapters.out.atividadeagendada.jpa;

import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtividadeAgendadaJpaRepository extends AtividadeAgendadaRepository, JpaRepository<AtividadeAgendada, AtividadeAgendadaId> {

    @Override
    @Query("SELECT a FROM AtividadeAgendada a JOIN FETCH a.atividadeConfig WHERE a.id = :id")
    Optional<AtividadeAgendada> findById(@Param("id") AtividadeAgendadaId id);

    @Override
    @Query("SELECT a FROM AtividadeAgendada a " +
           "JOIN FETCH a.atividadeConfig " +
           "WHERE a.jogador.id = :jogadorId " +
           "AND a.dataHoraInicio < :fim " +
           "AND a.dataHoraFim > :inicio")
    List<AtividadeAgendada> findByJogadorIdAndPeriodo(
            @Param("jogadorId") JogadorId jogadorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Override
    void deleteAll();
}
