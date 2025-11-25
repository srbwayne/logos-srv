package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.command.CreateRegraVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;

public interface CreateRegraVicioUseCase {
    RegraVicioDto create(CreateRegraVicioCommand command);
}
