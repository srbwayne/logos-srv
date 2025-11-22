package com.josecjuniors.logossrv.core.habilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

import java.util.UUID;

public interface CreateHabilidadeRequisitoUseCase {
    HabilidadeRequisitoDto create(UUID habilidadeId, TipoRequisito tipo, UUID requisitoId, Integer nivelMinimo);
}
