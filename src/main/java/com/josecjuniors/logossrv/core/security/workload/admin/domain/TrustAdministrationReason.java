package com.josecjuniors.logossrv.core.security.workload.admin.domain;

public record TrustAdministrationReason(String value) {
    public TrustAdministrationReason {
        if (value == null || value.isBlank() || value.length() > 512) {
            throw new IllegalArgumentException("reason must be nonblank and at most 512 characters");
        }
    }
}
