package com.josecjuniors.logossrv.core.appuser.domain.exception;

public class EmailJaCadastradoException extends IllegalStateException {
    public EmailJaCadastradoException() {
        // Mensagem genérica para não vazar informação se um e-mail existe ou não (OWASP).
        super("Não foi possível concluir o registro. Verifique os dados informados.");
    }
}
