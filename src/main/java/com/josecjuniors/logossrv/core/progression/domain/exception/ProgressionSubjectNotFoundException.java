package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionSubjectNotFoundException extends IllegalStateException {

    public ProgressionSubjectNotFoundException() {
        super("Estado de progressao nao encontrado para o subject.");
    }
}
