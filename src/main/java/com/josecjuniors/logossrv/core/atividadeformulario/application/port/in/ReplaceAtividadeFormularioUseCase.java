package com.josecjuniors.logossrv.core.atividadeformulario.application.port.in;

import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;

public interface ReplaceAtividadeFormularioUseCase {
    AtividadeFormularioJson replace(ReplaceAtividadeFormularioCommand command);
}
