package com.josecjuniors.logossrv.core.debuff.domain.exception;

public class RegraDistribuicaoDebuffNaoPertenceADebuffException extends SecurityException {
    public RegraDistribuicaoDebuffNaoPertenceADebuffException() {
        super("Esta regra de distribuição não pertence ao debuff informado.");
    }
}
