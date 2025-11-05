package com.josecjuniors.logossrv.core.habilidade.domain.exception;

public class HabilidadeNaoEncontradaException extends RuntimeException {
    public HabilidadeNaoEncontradaException() {
        super("Habilidade não encontrada.");
    }
}
