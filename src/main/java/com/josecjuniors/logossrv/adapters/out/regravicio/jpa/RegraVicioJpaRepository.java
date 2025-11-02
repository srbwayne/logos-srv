package com.josecjuniors.logossrv.adapters.out.regravicio.jpa;

import com.josecjuniors.logossrv.core.regravicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.regravicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.regravicio.domain.repository.RegraVicioRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegraVicioJpaRepository extends RegraVicioRepository, JpaRepository<RegraVicio, RegraVicioId> {
}
