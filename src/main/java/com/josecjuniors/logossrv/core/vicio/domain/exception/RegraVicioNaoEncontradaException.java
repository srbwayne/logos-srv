package com.josecjuniors.logossrv.core.vicio.domain.exception;

public class RegraVicioNaoEncontradaException extends RuntimeException {
    public RegraVicioNaoEncontradaException() {
        super("Regra Vício não encontrado.");
    }
}
