package com.josecjuniors.logossrv.core.registrovicio.application.port.in;

import com.josecjuniors.logossrv.core.registrovicio.application.command.CreateRegistroVicioCommand;
import com.josecjuniors.logossrv.core.registrovicio.application.dto.RegistroVicioDto;

public interface CreateRegistroVicioUseCase {
    RegistroVicioDto create(CreateRegistroVicioCommand command);
}
