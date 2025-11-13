package com.josecjuniors.logossrv.core.regrafatorxp.domain.exception;

public class RegraFatorXPNaoEncontradaException extends RuntimeException {
    public RegraFatorXPNaoEncontradaException() {
        super("Regra de Fator de XP não encontrada.");
    }
}
