package com.josecjuniors.logossrv.core.vicio.domain.repository;

import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VicioRepository {
    Vicio save(Vicio vicio);
    Optional<Vicio> findById(VicioId id);
    Page<Vicio> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, VicioId id);

    void deleteAll();
}
