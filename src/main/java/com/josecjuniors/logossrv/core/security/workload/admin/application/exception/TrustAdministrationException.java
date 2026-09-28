package com.josecjuniors.logossrv.core.security.workload.admin.application.exception;

import java.util.Objects;

public class TrustAdministrationException extends RuntimeException {
    private final TrustAdministrationError error;

    public TrustAdministrationException(TrustAdministrationError error) {
        super("Trust administration operation failed");
        this.error = Objects.requireNonNull(error, "error");
    }

    public TrustAdministrationException(TrustAdministrationError error, Throwable cause) {
        super("Trust administration operation failed", cause);
        this.error = Objects.requireNonNull(error, "error");
    }

    public TrustAdministrationError error() {
        return error;
    }
}
