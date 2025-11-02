package com.josecjuniors.logossrv.core.atributo.domain.exception;

import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;

public class AtributoNaoEncontradoException extends RuntimeException {
    public AtributoNaoEncontradoException(AtributoId id) {
        super("Atributo com o ID '" + id.getValue() + "' não foi encontrado.");
    }
}
