package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JogadorJpaRepository extends JogadorRepository, JpaRepository<Jogador, JogadorId> {

    @Override
    default Optional<Jogador> findByAppUserId(AppUserId appUserId){
        return findByUser_Id(appUserId);
    }

    @Override
    Optional<Jogador> findByUserEmail(String email);

    @Override
    boolean existsByApelidoAndIdNot(String apelido, JogadorId id);

    Optional<Jogador> findByUser_Id(AppUserId appUserId);
}
