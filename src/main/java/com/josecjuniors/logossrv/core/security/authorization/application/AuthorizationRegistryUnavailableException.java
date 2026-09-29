package com.josecjuniors.logossrv.core.security.authorization.application;

public final class AuthorizationRegistryUnavailableException extends RuntimeException {
    public AuthorizationRegistryUnavailableException(Throwable cause) {
        super("Authorization registry is unavailable.", cause);
    }
}
