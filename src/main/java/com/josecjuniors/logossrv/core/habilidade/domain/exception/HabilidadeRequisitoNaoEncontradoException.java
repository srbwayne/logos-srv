package com.josecjuniors.logossrv.core.habilidade.domain.exception;

public class HabilidadeRequisitoNaoEncontradoException extends RuntimeException {
    public HabilidadeRequisitoNaoEncontradoException() {
        super("Requisito de Habilidade não encontrado.");
    }
}
