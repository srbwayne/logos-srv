package com.josecjuniors.logossrv.core.atributo.domain.exception;

public class AtributoJaExisteException extends IllegalStateException {
    public AtributoJaExisteException(String nome) {
        super("Atributo com o nome '" + nome + "' já existe.");
    }
}
