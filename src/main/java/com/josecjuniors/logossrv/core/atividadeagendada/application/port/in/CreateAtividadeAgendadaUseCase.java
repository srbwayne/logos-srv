package com.josecjuniors.logossrv.core.atividadeagendada.application.port.in;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;

public interface CreateAtividadeAgendadaUseCase {
    AtividadeAgendadaDto create(CreateAtividadeAgendadaCommand command);
}
