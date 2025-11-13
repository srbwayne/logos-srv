package com.josecjuniors.logossrv.core.atividadeconfig.domain.repository;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AtividadeConfigRepository {
    AtividadeConfig save(AtividadeConfig atividade);
    Optional<AtividadeConfig> findById(AtividadeConfigId id);
    Page<AtividadeConfig> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, AtividadeConfigId id);
    void deleteById(AtividadeConfigId id);
    void deleteAll();
}
