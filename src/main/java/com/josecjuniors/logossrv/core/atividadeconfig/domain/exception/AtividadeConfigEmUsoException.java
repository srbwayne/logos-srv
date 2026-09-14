package com.josecjuniors.logossrv.core.atividadeconfig.domain.exception;

public class AtividadeConfigEmUsoException extends IllegalStateException {
    public AtividadeConfigEmUsoException() {
        super("Activity cannot be deleted while it has durable usage or progression state.");
    }
}
