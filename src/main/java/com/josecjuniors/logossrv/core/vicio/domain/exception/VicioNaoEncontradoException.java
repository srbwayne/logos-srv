package com.josecjuniors.logossrv.core.vicio.domain.exception;

public class VicioNaoEncontradoException extends RuntimeException {
    public VicioNaoEncontradoException() {
        super("Vício não encontrado.");
    }
}
