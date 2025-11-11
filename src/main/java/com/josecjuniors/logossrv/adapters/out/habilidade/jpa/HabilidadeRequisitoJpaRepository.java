package com.josecjuniors.logossrv.adapters.out.habilidade.jpa;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabilidadeRequisitoJpaRepository extends HabilidadeRequisitoRepository, JpaRepository<HabilidadeRequisito, HabilidadeRequisitoId> {

    @Override
    @Query("SELECT hr FROM HabilidadeRequisito hr " +
           "LEFT JOIN FETCH hr.atributoRequisito " +
           "LEFT JOIN FETCH hr.habilidadeRequisito " +
           "WHERE hr.habilidade.id = :habilidadeId")
    List<HabilidadeRequisito> findByHabilidadeId(@Param("habilidadeId") HabilidadeId habilidadeId);

    @Override
    @Query("SELECT hr FROM HabilidadeRequisito hr " +
           "LEFT JOIN FETCH hr.atributoRequisito " +
           "LEFT JOIN FETCH hr.habilidadeRequisito " +
           "WHERE hr.id = :requisitoId")
    Optional<HabilidadeRequisito> findById(@Param("requisitoId") HabilidadeRequisitoId requisitoId);
}
