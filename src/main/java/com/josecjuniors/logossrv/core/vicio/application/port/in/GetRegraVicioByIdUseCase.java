package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.command.BuscarRegravicioPorIdCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;

import java.util.Optional;

public interface GetRegraVicioByIdUseCase {
    Optional<RegraVicioDto> findById(BuscarRegravicioPorIdCommand command);
}
