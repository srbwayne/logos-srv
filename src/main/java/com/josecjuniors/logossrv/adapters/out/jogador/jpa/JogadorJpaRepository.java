package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.HabilidadeJogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface JogadorJpaRepository extends JogadorRepository, JpaRepository<Jogador, JogadorId> {

    @Override
    Jogador save(Jogador jogador);

    @Override
    Optional<Jogador> findById(JogadorId id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jogador j where j.id = :jogadorId")
    Optional<Jogador> findByIdForUpdate(@Param("jogadorId") JogadorId id);

    @Override
    default Optional<Jogador> findByAppUserId(AppUserId appUserId){
        return findByUser_Id(appUserId);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jogador j where j.user.id = :appUserId")
    Optional<Jogador> findByAppUserIdForUpdate(@Param("appUserId") AppUserId appUserId);

    @Override
    Optional<Jogador> findByUserEmail(String email);

    @Override
    boolean existsByApelidoAndIdNot(String apelido, JogadorId id);

    @Override
    void deleteAll();

    Optional<Jogador> findByUser_Id(AppUserId appUserId);

}
