package com.josecjuniors.logossrv.core.security.workload.application.exception;

public class WorkloadAssertionReplayException extends RuntimeException {
    public WorkloadAssertionReplayException() {
        super("Workload assertion authentication failed");
    }
}
