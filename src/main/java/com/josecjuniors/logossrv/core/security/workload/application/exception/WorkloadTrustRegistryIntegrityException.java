package com.josecjuniors.logossrv.core.security.workload.application.exception;

public class WorkloadTrustRegistryIntegrityException extends RuntimeException {
    public WorkloadTrustRegistryIntegrityException(String reason) {
        super("Workload trust registry integrity failure: " + reason);
    }

    public WorkloadTrustRegistryIntegrityException(String reason, Throwable cause) {
        super("Workload trust registry integrity failure: " + reason, cause);
    }
}
