package com.josecjuniors.logossrv.core.registrovicio.domain.exception;

public class RegistroVicioNaoEncontradoException extends RuntimeException {
    public RegistroVicioNaoEncontradoException() {
        super("Registro de Vício não encontrado.");
    }
}
