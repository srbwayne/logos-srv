package com.josecjuniors.logossrv.core.habilidade.domain.exception;

public class RequisitoNaoPertenceAEstaHabilidadeException extends SecurityException {
    public RequisitoNaoPertenceAEstaHabilidadeException() {
        super("O requisito não pertence à habilidade informada.");
    }
}
