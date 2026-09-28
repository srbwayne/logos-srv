package com.josecjuniors.logossrv.core.security.authorization.domain;

import java.util.Objects;
import java.util.Optional;

public record AuthorizationGrant(
        AuthorizationPrincipal principal,
        AuthorizationOperation operation,
        Optional<AuthorizationSource> source,
        Optional<AuthorizationNamespace> namespace) {

    public AuthorizationGrant {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(namespace, "namespace");
        if (source.isPresent() != operation.requiresSource()
                || namespace.isPresent() != operation.requiresNamespace()) {
            throw new IllegalArgumentException("grant dimensions must match operation applicability");
        }
    }
}
