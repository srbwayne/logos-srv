package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

import java.util.Optional;

public interface GetAtividadeConfigByIdUseCase {
    Optional<AtividadeConfigDto> getById(AtividadeConfigId id);
}
