package com.josecjuniors.logossrv.core.atividadeconfig.domain.exception;

public class AtividadeConfigJaExisteException extends IllegalStateException {
    public AtividadeConfigJaExisteException(String nome) {
        super("Configuração de Atividade com o nome '" + nome + "' já existe.");
    }
}
