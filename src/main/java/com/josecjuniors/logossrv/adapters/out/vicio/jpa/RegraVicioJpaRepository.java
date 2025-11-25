package com.josecjuniors.logossrv.adapters.out.vicio.jpa;

import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegraVicioJpaRepository extends RegraVicioRepository, JpaRepository<RegraVicio, RegraVicioId> {

    @Override
    RegraVicio save(RegraVicio regra);

    @Override
    void delete(RegraVicio regra);

    @Override
    @Query("SELECT r FROM RegraVicio r LEFT JOIN FETCH r.debuff WHERE r.vicio.id = :vicioId")
    List<RegraVicio> findAllByVicioId(@Param("vicioId") VicioId vicioId);

    @Override
    @Query("SELECT r FROM RegraVicio r LEFT JOIN FETCH r.debuff WHERE r.id = :id")
    Optional<RegraVicio> findById(@Param("id") RegraVicioId id);

    @Override
    void deleteAll();
}
