package com.josecjuniors.logossrv.core.atividadeagendada.application.port.in;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;

public interface ReagendarAtividadeUseCase {
    AtividadeAgendadaDto reagendar(ReagendarAtividadeCommand command);
}
