package com.josecjuniors.logossrv.adapters.out.atividadeconfig.jpa;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtividadeConfigJpaRepository extends AtividadeConfigRepository, JpaRepository<AtividadeConfig, AtividadeConfigId> {
}
