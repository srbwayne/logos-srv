package com.josecjuniors.logossrv.core.registrovicio.domain.repository;

import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;

import java.util.Optional;

public interface RegistroVicioRepository {
    RegistroVicio save(RegistroVicio registroVicio);

    void deleteAll();

    Optional<RegistroVicio> findById(RegistroVicioId id);
}
