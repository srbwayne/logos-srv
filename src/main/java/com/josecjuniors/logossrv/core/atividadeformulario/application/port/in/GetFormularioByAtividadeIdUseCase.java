package com.josecjuniors.logossrv.core.atividadeformulario.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.AtividadeFormularioJson;

import java.util.Optional;

public interface GetFormularioByAtividadeIdUseCase {
    Optional<AtividadeFormularioJson> getByAtividadeId(AtividadeConfigId atividadeId);
}
