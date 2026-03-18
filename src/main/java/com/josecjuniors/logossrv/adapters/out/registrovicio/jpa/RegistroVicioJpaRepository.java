package com.josecjuniors.logossrv.adapters.out.registrovicio.jpa;

import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;
import com.josecjuniors.logossrv.core.registrovicio.domain.repository.RegistroVicioRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistroVicioJpaRepository extends RegistroVicioRepository, JpaRepository<RegistroVicio, RegistroVicioId> {
    @Override
    RegistroVicio save(RegistroVicio registroVicio);

    @Override
    void deleteAll();

    @Override
    Optional<RegistroVicio> findById(RegistroVicioId id);

}
