package com.josecjuniors.logossrv.core.atributo.domain.exception;

public class AtributoNaoEncontradoException extends RuntimeException {
    public AtributoNaoEncontradoException() {
        super("Atributo não encontrado.");
    }
}
