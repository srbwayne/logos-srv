package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;

import java.util.List;

public interface GetAllHabilidadesUseCase {
    List<HabilidadeDto> getAllHabilidades();
}
