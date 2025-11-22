package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.exception;

public class RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException extends SecurityException {
    public RegradistribuicaoHabilidadeNaoPertenceAEstaHabilidadeException() {
        super("A Regra de distribuição não pertence à habilidade informada.");
    }
}