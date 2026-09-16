package com.josecjuniors.logossrv.core.atividadeformulario.domain.exception;

public class AtividadeFormularioConfiguracaoException extends IllegalStateException {
    private final String code;

    public AtividadeFormularioConfiguracaoException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
