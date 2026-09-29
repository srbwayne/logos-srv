package com.josecjuniors.logossrv.core.security.authorization.application;

public final class AuthorizationRegistryCorruptedException extends RuntimeException {
    public AuthorizationRegistryCorruptedException(Throwable cause) {
        super("Authorization registry data is corrupted.", cause);
    }
}
