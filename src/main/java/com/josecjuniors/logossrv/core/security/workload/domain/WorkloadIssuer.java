package com.josecjuniors.logossrv.core.security.workload.domain;

public record WorkloadIssuer(String value) {
    public WorkloadIssuer {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException("Workload issuer must be nonblank and at most 255 characters");
        }
    }
}
