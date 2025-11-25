package com.josecjuniors.logossrv.core.debuff.domain.exception;

public class DebuffNaoEncontradoException extends RuntimeException {
    public DebuffNaoEncontradoException() {
        super("Debuff não encontrado.");
    }
}
