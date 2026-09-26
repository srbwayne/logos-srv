package com.josecjuniors.logossrv.core.security.workload.application;

public class WorkloadAssertionVerificationException extends RuntimeException {
    private final WorkloadAssertionFailureReason reason;

    public WorkloadAssertionVerificationException(WorkloadAssertionFailureReason reason) {
        super("Workload assertion verification failed");
        this.reason = reason;
    }

    public WorkloadAssertionVerificationException(WorkloadAssertionFailureReason reason, Throwable cause) {
        super("Workload assertion verification failed", cause);
        this.reason = reason;
    }

    public WorkloadAssertionFailureReason reason() {
        return reason;
    }
}
