package com.josecjuniors.logossrv.core.progression.domain.model;

public enum ProgressionExecutionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public static ProgressionExecutionStatus fromPersistence(String value) {
        try {
            return valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unknown progression execution status", exception);
        }
    }
}
