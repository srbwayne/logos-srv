package com.josecjuniors.logossrv.core.atributo.domain.exception;

public class AtributoSemanticKeyJaExisteException extends IllegalStateException {
    public AtributoSemanticKeyJaExisteException(String semanticKey) {
        super("A semantic key already exists: " + semanticKey);
    }
}
