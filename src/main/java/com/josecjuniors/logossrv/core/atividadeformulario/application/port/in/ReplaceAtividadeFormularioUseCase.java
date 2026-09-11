package com.josecjuniors.logossrv.core.atividadeformulario.application.port.in;

import com.josecjuniors.logossrv.core.atividadeformulario.application.dto.AtividadeFormularioDto;

public interface ReplaceAtividadeFormularioUseCase {
    AtividadeFormularioDto replace(ReplaceAtividadeFormularioCommand command);
}
