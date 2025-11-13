package com.josecjuniors.logossrv.core.fatorcalculo.domain.exception;

public class FatorCalculoNaoEncontradoException extends RuntimeException {
    
    /**
     * Construtor padrão com mensagem genérica para não vazar informações.
     */
    public FatorCalculoNaoEncontradoException() {
        super("Fator de Cálculo não encontrado.");
    }
}
