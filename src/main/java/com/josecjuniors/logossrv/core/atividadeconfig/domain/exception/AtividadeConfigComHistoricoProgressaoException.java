package com.josecjuniors.logossrv.core.atividadeconfig.domain.exception;

public class AtividadeConfigComHistoricoProgressaoException extends IllegalStateException {
    public AtividadeConfigComHistoricoProgressaoException() {
        super("Activity with legacy progression history cannot be deleted.");
    }
}
