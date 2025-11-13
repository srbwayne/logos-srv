package com.josecjuniors.logossrv.core.fatorcalculo.domain.exception;

public class FatorCalculoJaExisteException extends IllegalStateException {
    public FatorCalculoJaExisteException(String nome) {
        super("Fator de Cálculo com o nome '" + nome + "' já existe.");
    }
}
