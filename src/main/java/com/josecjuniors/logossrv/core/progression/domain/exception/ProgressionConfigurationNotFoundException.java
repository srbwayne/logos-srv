package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionConfigurationNotFoundException extends IllegalStateException {

    public ProgressionConfigurationNotFoundException() {
        super("Configuracao de progressao nao encontrada.");
    }
}
