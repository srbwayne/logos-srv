package com.josecjuniors.logossrv.core.fatorcalculo.domain.exception;

public class SemanticKeyAlreadyAssignedException extends IllegalStateException {
    public SemanticKeyAlreadyAssignedException() {
        super("semanticKey já atribuído e não pode ser alterado.");
    }
}
