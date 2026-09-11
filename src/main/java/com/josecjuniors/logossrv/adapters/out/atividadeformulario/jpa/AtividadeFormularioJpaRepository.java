package com.josecjuniors.logossrv.adapters.out.atividadeformulario.jpa;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormularioId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtividadeFormularioJpaRepository extends AtividadeFormularioRepository, JpaRepository<AtividadeFormulario, AtividadeFormularioId> {
    
    @Override
    Optional<AtividadeFormulario> findByAtividadeConfigId(AtividadeConfigId atividadeConfigId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from AtividadeFormulario f where f.atividadeConfig.id = :atividadeConfigId")
    Optional<AtividadeFormulario> findByAtividadeConfigIdForUpdate(@Param("atividadeConfigId") AtividadeConfigId atividadeConfigId);

    @Override
    void deleteAll(); // Adicionado
}
