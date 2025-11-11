package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;

import java.util.List;

public interface GetRequisitosDaHabilidadeUseCase {
    List<HabilidadeRequisitoDto> getRequisitos(HabilidadeId habilidadeId);
}
