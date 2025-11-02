package com.josecjuniors.logossrv.core.atributo.application.port.in;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;

public interface UpdateAtributoUseCase {
    AtributoDto updateAtributo(UpdateAtributoCommand command);
}
