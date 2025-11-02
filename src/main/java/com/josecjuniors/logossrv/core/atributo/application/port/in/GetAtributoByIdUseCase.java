package com.josecjuniors.logossrv.core.atributo.application.port.in;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;

import java.util.Optional;

public interface GetAtributoByIdUseCase {
    Optional<AtributoDto> getAtributoById(GetAtributoByIdCommand command);
}
