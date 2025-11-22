package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.DeletarHabilidadeRequisitoCommand;

public interface DeleteHabilidadeRequisitoUseCase {
    void delete(DeletarHabilidadeRequisitoCommand command);
}
