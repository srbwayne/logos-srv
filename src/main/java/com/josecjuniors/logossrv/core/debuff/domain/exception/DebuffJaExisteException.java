package com.josecjuniors.logossrv.core.debuff.domain.exception;

public class DebuffJaExisteException extends RuntimeException {
    public DebuffJaExisteException() {
        super("Já existe um debuff com este nome.");
    }
}
