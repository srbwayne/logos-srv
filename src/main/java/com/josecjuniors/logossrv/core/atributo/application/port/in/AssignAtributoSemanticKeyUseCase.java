package com.josecjuniors.logossrv.core.atributo.application.port.in;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;

public interface AssignAtributoSemanticKeyUseCase {
    AtributoDto assign(AtributoId id, String semanticKey);
}
