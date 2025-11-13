package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.exception;

public class RegraDistribuicaoNaoEncontradaException extends RuntimeException {
    public RegraDistribuicaoNaoEncontradaException() {
        super("Regra de Distribuição não encontrada.");
    }
}
