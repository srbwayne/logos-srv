package com.josecjuniors.logossrv.core.debuff.domain.exception;

public class RegraDistribuicaoDebuffNaoEncontradaException extends RuntimeException {
    public RegraDistribuicaoDebuffNaoEncontradaException() {
        super("Regra de distribuição de debuff não encontrada.");
    }
}
