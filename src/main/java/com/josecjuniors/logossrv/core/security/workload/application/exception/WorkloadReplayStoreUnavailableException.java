package com.josecjuniors.logossrv.core.security.workload.application.exception;

public class WorkloadReplayStoreUnavailableException extends RuntimeException {
    public WorkloadReplayStoreUnavailableException() {
        super("Workload replay protection is unavailable");
    }

    public WorkloadReplayStoreUnavailableException(Throwable cause) {
        super("Workload replay protection is unavailable", cause);
    }
}
