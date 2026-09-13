package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionConfigurationNotActiveException extends IllegalStateException {

    public ProgressionConfigurationNotActiveException() {
        super("Progression configuration is not active for execution.");
    }
}
