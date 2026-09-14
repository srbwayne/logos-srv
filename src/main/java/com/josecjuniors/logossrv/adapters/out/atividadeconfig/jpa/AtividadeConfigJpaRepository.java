package com.josecjuniors.logossrv.adapters.out.atividadeconfig.jpa;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtividadeConfigJpaRepository extends AtividadeConfigRepository, JpaRepository<AtividadeConfig, AtividadeConfigId> {
    
    @Override
    AtividadeConfig save(AtividadeConfig atividade);

    @Override
    Optional<AtividadeConfig> findById(AtividadeConfigId id);

    @Override
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AtividadeConfig a where a.id = :id")
    Optional<AtividadeConfig> findByIdForUpdate(@Param("id") AtividadeConfigId id);

    @Override
    Page<AtividadeConfig> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, AtividadeConfigId id);

    @Override
    void deleteById(AtividadeConfigId id);

    @Override
    void deleteAll();
}
