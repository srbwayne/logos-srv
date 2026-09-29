package com.josecjuniors.logossrv.core.security.authorization.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;

import java.util.Objects;
import java.util.Optional;

public final class AuthorizationEvaluator {
    public enum AuthorizationDecision { ALLOW, DENY }

    private final AuthorizationGrantStore grantStore;

    public AuthorizationEvaluator(AuthorizationGrantStore grantStore) {
        this.grantStore = Objects.requireNonNull(grantStore, "grantStore");
    }

    public AuthorizationDecision evaluate(AuthenticatedPrincipal principal, AuthorizationOperation operation,
                                          Optional<AuthorizationSource> source,
                                          Optional<AuthorizationNamespace> namespace) {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(namespace, "namespace");
        if (principal.authenticationStatus() != AuthenticationStatus.VERIFIED) {
            return AuthorizationDecision.DENY;
        }
        AuthorizationPrincipal authorizationPrincipal = new AuthorizationPrincipal(
                principal.principalType(), principal.principalId());
        AuthorizationGrant lookup = new AuthorizationGrant(authorizationPrincipal, operation, source, namespace);
        return grantStore.findExact(lookup).isPresent() ? AuthorizationDecision.ALLOW : AuthorizationDecision.DENY;
    }
}
