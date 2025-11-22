package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception;

public class RegraDistribuicaoHabilidadeNaoEncontradaException extends RuntimeException {
    public RegraDistribuicaoHabilidadeNaoEncontradaException() {
        super("Regra de Distribuição de Habilidade não encontrada.");
    }
}
