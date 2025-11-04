package com.josecjuniors.logossrv.core.jogador.domain.exception;

public class ApelidoJaEmUsoException extends IllegalStateException {
    public ApelidoJaEmUsoException() {
        // Mensagem genérica para não vazar informação se um apelido existe ou não.
        super("Não foi possível atualizar o perfil. Verifique os dados informados.");
    }
}
