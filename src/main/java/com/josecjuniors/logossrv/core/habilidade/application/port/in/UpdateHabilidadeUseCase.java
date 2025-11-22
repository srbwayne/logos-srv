package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.UpdateHabilidadeCommand;

public interface UpdateHabilidadeUseCase {
    HabilidadeDto updateHabilidade(UpdateHabilidadeCommand command);
}
