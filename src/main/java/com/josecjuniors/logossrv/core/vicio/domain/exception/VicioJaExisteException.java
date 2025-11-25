package com.josecjuniors.logossrv.core.vicio.domain.exception;

public class VicioJaExisteException extends RuntimeException {
    public VicioJaExisteException() {
        super("Já existe um vício com este nome.");
    }
}
