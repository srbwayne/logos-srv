package com.josecjuniors.logossrv.core.regrafatorestresse.domain.exception;

public class RegraFatorEstresseNaoEncontradaException extends RuntimeException {
    public RegraFatorEstresseNaoEncontradaException() {
        super("Regra de Fator de Estresse não encontrada.");
    }
}
