package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;

public interface UpdateFatorCalculoUseCase {
    FatorCalculoDto update(UpdateFatorCalculoCommand command);
}
