package com.josecjuniors.logossrv.core.jogador.domain.exception;

public class VicioJogadorNaoEncontradoException extends RuntimeException {
    public VicioJogadorNaoEncontradoException() {
        super("O jogador não está lutando contra este vício ou o vício não existe.");
    }
}
