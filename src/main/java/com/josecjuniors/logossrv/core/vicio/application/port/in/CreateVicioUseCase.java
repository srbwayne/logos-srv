package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.command.CreateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;

public interface CreateVicioUseCase {
    VicioDto create(CreateVicioCommand command);
}
