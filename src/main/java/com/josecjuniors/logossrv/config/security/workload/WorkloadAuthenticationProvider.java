package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionVerificationException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadAssertionReplayException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadReplayStoreUnavailableException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/** Explicitly constructed by a dedicated workload AuthenticationManager; not a Spring bean. */
public final class WorkloadAuthenticationProvider implements AuthenticationProvider {
    private final WorkloadAuthenticationService workloadAuthenticationService;

    public WorkloadAuthenticationProvider(WorkloadAuthenticationService workloadAuthenticationService) {
        this.workloadAuthenticationService = workloadAuthenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof WorkloadAssertionAuthenticationToken request)) {
            throw new BadCredentialsException("Unauthorized");
        }
        Object credentials = request.getCredentials();
        if (!(credentials instanceof String assertion) || assertion.isEmpty()) {
            throw new BadCredentialsException("Unauthorized");
        }
        try {
            AuthenticatedPrincipal principal = workloadAuthenticationService.authenticate(assertion);
            return WorkloadPrincipalAuthenticationToken.authenticated(principal);
        } catch (WorkloadAssertionVerificationException | WorkloadAssertionReplayException exception) {
            throw new BadCredentialsException("Unauthorized");
        } catch (WorkloadReplayStoreUnavailableException | WorkloadTrustRegistryIntegrityException exception) {
            throw new AuthenticationServiceException("Authentication service unavailable");
        } catch (RuntimeException exception) {
            throw new AuthenticationServiceException("Authentication service unavailable");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WorkloadAssertionAuthenticationToken.class.equals(authentication);
    }
}
