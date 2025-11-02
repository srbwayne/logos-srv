package com.josecjuniors.logossrv.core.regravicio.domain.repository;

import com.josecjuniors.logossrv.core.regravicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.regravicio.domain.model.RegraVicioId;

import java.util.Optional;

public interface RegraVicioRepository {

    RegraVicio save(RegraVicio regraVicio);

    Optional<RegraVicio> findById(RegraVicioId id);

    void delete(RegraVicio regraVicio);
}
