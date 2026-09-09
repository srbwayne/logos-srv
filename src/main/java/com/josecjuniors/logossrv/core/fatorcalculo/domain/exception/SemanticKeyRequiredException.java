package com.josecjuniors.logossrv.core.fatorcalculo.domain.exception;

public class SemanticKeyRequiredException extends IllegalStateException {

    public SemanticKeyRequiredException() {
        super("Novas versões de configuração exigem semanticKey nos fatores referenciados.");
    }
}
