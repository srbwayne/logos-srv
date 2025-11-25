package com.josecjuniors.logossrv.core.vicio.domain.repository;

import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

import java.util.List;
import java.util.Optional;

public interface RegraVicioRepository {
    RegraVicio save(RegraVicio regra);
    void delete(RegraVicio regra);
    Optional<RegraVicio> findById(RegraVicioId id);
    List<RegraVicio> findAllByVicioId(VicioId vicioId);

    void deleteAll();
}
