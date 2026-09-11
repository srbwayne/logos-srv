package com.josecjuniors.logossrv.core.atividadeformulario.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.application.dto.AtividadeFormularioDto;

import java.util.Optional;

public interface GetFormularioByAtividadeIdUseCase {
    Optional<AtividadeFormularioDto> getByAtividadeId(AtividadeConfigId atividadeId);
}
