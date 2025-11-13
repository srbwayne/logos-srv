package com.josecjuniors.logossrv.core.atividadeconfig.domain.exception;

public class AtividadeConfigNaoEncontradaException extends RuntimeException {
    public AtividadeConfigNaoEncontradaException() {
        super("Configuração de Atividade não encontrada.");
    }
}
