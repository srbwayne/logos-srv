package com.josecjuniors.logossrv.core.habilidade.domain.exception;

public class HabilidadeJaExisteException extends IllegalStateException {
    public HabilidadeJaExisteException(String nome) {
        super("Habilidade com o nome '" + nome + "' já existe.");
    }
}
