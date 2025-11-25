package com.josecjuniors.logossrv.core.vicio.application.port.in;

import com.josecjuniors.logossrv.core.vicio.application.command.DeletarRegravicioCommand;

public interface DeleteRegraVicioUseCase {
    void delete(DeletarRegravicioCommand command);
}
