package com.josecjuniors.logossrv.config.security.workload;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

/** Pre-authentication container only; the assertion is never a principal. */
public final class WorkloadAssertionAuthenticationToken extends AbstractAuthenticationToken {
    private final String compactAssertion;

    private WorkloadAssertionAuthenticationToken(String compactAssertion) {
        super(List.of());
        if (compactAssertion == null || compactAssertion.isEmpty()) {
            throw new IllegalArgumentException("A workload assertion is required");
        }
        this.compactAssertion = compactAssertion;
    }

    static WorkloadAssertionAuthenticationToken unauthenticated(String compactAssertion) {
        return new WorkloadAssertionAuthenticationToken(compactAssertion);
    }

    @Override
    public Object getCredentials() {
        return compactAssertion;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Use the workload authentication provider to authenticate");
        }
        super.setAuthenticated(false);
    }
}
