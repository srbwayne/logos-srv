package com.josecjuniors.logossrv.adapters.out.registroatividade.jpa;

import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAtividadeJpaRepository extends RegistroAtividadeRepository, JpaRepository<RegistroAtividade, RegistroAtividadeId> {
    @Override
    RegistroAtividade save(RegistroAtividade registro);

    @Override
    Optional<RegistroAtividade> findById(RegistroAtividadeId id);

    @Override
    @Query("SELECT r FROM RegistroAtividade r " +
           "LEFT JOIN FETCH r.detalhes d " +
           "LEFT JOIN FETCH d.fatorCalculo " +
           "LEFT JOIN FETCH r.atividadeConfig ac " +
           "LEFT JOIN FETCH ac.regrasDistribuicao rd " +
           "LEFT JOIN FETCH rd.regraFatorXPS rfx " +
           "LEFT JOIN FETCH rd.regraFatorEstresses rfe " +
           "WHERE r.id = :id")
    Optional<RegistroAtividade> findByIdWithDetails(@Param("id") RegistroAtividadeId id);

    @Override
    void deleteAll();

    @Override
    List<RegistroAtividade> findAll();
}
