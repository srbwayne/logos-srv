package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionFactKeyNotRepresentedException extends IllegalStateException {
    public ProgressionFactKeyNotRepresentedException(String factorId) {
        super("O fator numérico " + factorId + " não é representado pela versão congelada da configuração.");
    }
}
