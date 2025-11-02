package com.josecjuniors.logossrv.core.atividadeconfig.domain.repository;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

import java.util.Optional;

public interface AtividadeConfigRepository {

    AtividadeConfig save(AtividadeConfig atividadeConfig);

    Optional<AtividadeConfig> findById(AtividadeConfigId id);

    void delete(AtividadeConfig atividadeConfig);
}
