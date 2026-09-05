package com.josecjuniors.logossrv.core.progression.domain.exception;

public class ProgressionExecutionConflictException extends IllegalStateException {
    public ProgressionExecutionConflictException() {
        super("Idempotency identity was already used with a different request.");
    }
}
