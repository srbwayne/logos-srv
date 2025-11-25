package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.command.UpdateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;

public interface UpdateVicioUseCase {
    VicioDto update(UpdateVicioCommand command);
}
