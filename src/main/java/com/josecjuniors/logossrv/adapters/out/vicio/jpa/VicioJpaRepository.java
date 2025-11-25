package com.josecjuniors.logossrv.adapters.out.vicio.jpa;

import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VicioJpaRepository extends VicioRepository, JpaRepository<Vicio, VicioId> {

    @Override
    Vicio save(Vicio vicio);

    @Override
    Optional<Vicio> findById(VicioId id);


    @Override
    @Query("SELECT v FROM Vicio v WHERE (:nome IS NULL OR v.nome LIKE %:nome%)")
    Page<Vicio> findByNomeContainingIgnoreCase(@Param("nome") String nome, Pageable pageable);

    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, VicioId id);

    @Override
    void deleteAll();
}
