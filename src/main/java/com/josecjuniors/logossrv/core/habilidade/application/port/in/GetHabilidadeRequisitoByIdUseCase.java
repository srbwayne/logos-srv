package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;

import java.util.Optional;

public interface GetHabilidadeRequisitoByIdUseCase {
    Optional<GetHabilidadeRequisitoByIdDto> getById(HabilidadeRequisitoId requisitoId);
}
