package com.josecjuniors.logossrv.core.jogador.domain.exception;

public class JogadorNaoEncontradoException extends RuntimeException {
    public JogadorNaoEncontradoException() {
        super("Jogador não encontrado para o usuário logado.");
    }
}
