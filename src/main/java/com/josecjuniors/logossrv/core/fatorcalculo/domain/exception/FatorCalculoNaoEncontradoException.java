package com.josecjuniors.logossrv.core.fatorcalculo.domain.exception;

import java.text.MessageFormat;

public class FatorCalculoNaoEncontradoException extends RuntimeException {
    public FatorCalculoNaoEncontradoException() {
        super("Fator de Cálculo não encontrado.");
    }

    public FatorCalculoNaoEncontradoException(String valor) {
        super(MessageFormat.format("Fator de Cálculo, {0}, não encontrado.", valor));
    }
}
