package com.josecjuniors.logossrv.core.fatorcalculo.application.port.in;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;

public interface AssignFatorCalculoSemanticKeyUseCase {
    FatorCalculoDto assign(FatorCalculoId id, String semanticKey);
}
