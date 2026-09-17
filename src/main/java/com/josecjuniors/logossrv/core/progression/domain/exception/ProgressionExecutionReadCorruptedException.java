package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionExecutionReadCorruptedException extends IllegalStateException {
    public ProgressionExecutionReadCorruptedException() {
        super("Progression execution data is corrupted.");
    }

    public ProgressionExecutionReadCorruptedException(Throwable cause) {
        super("Progression execution data is corrupted.", cause);
    }
}
