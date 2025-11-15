package com.josecjuniors.logossrv.core.atividadeagendada.domain.exception;

public class AtividadeAgendadaNaoEncontradaException extends RuntimeException {
    public AtividadeAgendadaNaoEncontradaException() {
        super("Atividade Agendada não encontrada.");
    }
}
