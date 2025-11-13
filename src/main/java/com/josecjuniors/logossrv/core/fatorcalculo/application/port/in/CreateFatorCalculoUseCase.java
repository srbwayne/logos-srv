package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;

public interface CreateFatorCalculoUseCase {
    FatorCalculoDto create(CreateFatorCalculoCommand command);
}
