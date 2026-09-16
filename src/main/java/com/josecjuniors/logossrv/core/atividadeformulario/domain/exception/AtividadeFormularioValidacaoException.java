package com.josecjuniors.logossrv.core.atividadeformulario.domain.exception;

public class AtividadeFormularioValidacaoException extends IllegalArgumentException {
    private final String code;

    public AtividadeFormularioValidacaoException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
