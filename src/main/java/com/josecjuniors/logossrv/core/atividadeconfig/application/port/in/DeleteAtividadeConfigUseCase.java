package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

public interface DeleteAtividadeConfigUseCase {
    void delete(AtividadeConfigId id);
}
