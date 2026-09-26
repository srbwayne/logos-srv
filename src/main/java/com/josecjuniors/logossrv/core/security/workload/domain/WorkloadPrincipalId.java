package com.josecjuniors.logossrv.core.security.workload.domain;

public record WorkloadPrincipalId(String value) {
    public WorkloadPrincipalId {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Workload principal id must be nonblank and at most 128 characters");
        }
    }
}
