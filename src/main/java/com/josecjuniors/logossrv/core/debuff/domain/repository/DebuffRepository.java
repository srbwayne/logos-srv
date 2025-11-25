package com.josecjuniors.logossrv.core.debuff.domain.repository;

import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DebuffRepository {
    Debuff save(Debuff debuff);
    Optional<Debuff> findById(DebuffId id);
    Page<Debuff> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    List<Debuff> findAll();
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, DebuffId id);

    void deleteAll();
}
