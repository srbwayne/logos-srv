package com.josecjuniors.logossrv.core.atributo.domain.exception;

public class AtributoSemanticKeyJaAtribuidaException extends IllegalStateException {
    public AtributoSemanticKeyJaAtribuidaException() {
        super("A semantic key is already assigned to this atributo");
    }
}
