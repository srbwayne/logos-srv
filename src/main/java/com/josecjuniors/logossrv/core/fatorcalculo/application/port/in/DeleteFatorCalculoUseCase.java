package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;

public interface DeleteFatorCalculoUseCase {
    void delete(FatorCalculoId id);
}
