package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;

public interface CreateAtividadeConfigUseCase {
    AtividadeConfigDto create(CreateAtividadeConfigCommand command);
}
