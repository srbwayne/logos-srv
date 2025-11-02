package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;

public interface UpdateHabilidadeUseCase {
    HabilidadeDto updateHabilidade(UpdateHabilidadeCommand command);
}
