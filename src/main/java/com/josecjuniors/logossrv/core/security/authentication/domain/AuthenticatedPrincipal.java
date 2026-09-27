package com.josecjuniors.logossrv.core.security.authentication.domain;

import java.util.Objects;

public record AuthenticatedPrincipal(
        PrincipalType principalType,
        String principalId,
        AuthenticationMethod authenticationMethod,
        String credentialIdentity,
        AuthenticationStatus authenticationStatus) {
    public AuthenticatedPrincipal {
        Objects.requireNonNull(principalType, "principalType");
        Objects.requireNonNull(authenticationMethod, "authenticationMethod");
        Objects.requireNonNull(authenticationStatus, "authenticationStatus");
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("principalId must be nonblank");
        }
        if (credentialIdentity == null || credentialIdentity.isBlank()) {
            throw new IllegalArgumentException("credentialIdentity must be nonblank");
        }
    }
}
