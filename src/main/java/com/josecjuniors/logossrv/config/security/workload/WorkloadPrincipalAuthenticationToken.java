package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.Objects;

/** Successful workload identity. Credentials are deliberately absent. */
public final class WorkloadPrincipalAuthenticationToken extends AbstractAuthenticationToken {
    private final AuthenticatedPrincipal principal;

    private WorkloadPrincipalAuthenticationToken(AuthenticatedPrincipal principal) {
        super(List.of());
        this.principal = Objects.requireNonNull(principal, "principal");
        super.setAuthenticated(true);
    }

    static WorkloadPrincipalAuthenticationToken authenticated(AuthenticatedPrincipal principal) {
        return new WorkloadPrincipalAuthenticationToken(principal);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.principalId();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Use the workload authentication provider to authenticate");
        }
        super.setAuthenticated(false);
    }
}
