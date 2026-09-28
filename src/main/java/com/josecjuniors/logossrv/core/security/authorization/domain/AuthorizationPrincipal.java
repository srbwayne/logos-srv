package com.josecjuniors.logossrv.core.security.authorization.domain;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;

import java.util.Objects;

public record AuthorizationPrincipal(PrincipalType principalType, String principalId) {
    public AuthorizationPrincipal {
        Objects.requireNonNull(principalType, "principalType");
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("principalId must be nonblank");
        }
        if (!principalId.equals(principalId.trim())) {
            throw new IllegalArgumentException("principalId must already be trimmed");
        }
        if (principalId.length() > 128) {
            throw new IllegalArgumentException("principalId must have at most 128 characters");
        }
    }
}
