package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.BuscarHabilidadeRequisitoPorIdCommand;

import java.util.Optional;

public interface GetHabilidadeRequisitoByIdUseCase {
    Optional<GetHabilidadeRequisitoByIdDto> getById(BuscarHabilidadeRequisitoPorIdCommand command);
}
