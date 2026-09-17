package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionExecutionNotFoundException extends IllegalStateException {
    public ProgressionExecutionNotFoundException() {
        super("Progression execution was not found.");
    }
}
