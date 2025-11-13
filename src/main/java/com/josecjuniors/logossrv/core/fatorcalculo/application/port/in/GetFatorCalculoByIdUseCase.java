package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;

import java.util.Optional;

public interface GetFatorCalculoByIdUseCase {
    Optional<FatorCalculoDto> getById(FatorCalculoId id);
}
