package com.josecjuniors.logossrv.core.security.workload.admin.application.exception;

public final class InvalidWorkloadPublicKeyException extends TrustAdministrationException {
    public InvalidWorkloadPublicKeyException(Throwable cause) {
        super(TrustAdministrationError.INVALID_PUBLIC_KEY, cause);
    }
}
