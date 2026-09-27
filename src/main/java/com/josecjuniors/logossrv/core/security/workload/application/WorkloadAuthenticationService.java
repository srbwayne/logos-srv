package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import org.springframework.stereotype.Service;

@Service
public class WorkloadAuthenticationService {
    private final WorkloadAssertionVerifier verifier;
    private final WorkloadAssertionReplayGuard replayGuard;

    public WorkloadAuthenticationService(WorkloadAssertionVerifier verifier,
                                         WorkloadAssertionReplayGuard replayGuard) {
        this.verifier = verifier;
        this.replayGuard = replayGuard;
    }

    public AuthenticatedPrincipal authenticate(String compactAssertion) {
        VerifiedWorkloadAssertion verified = verifier.verify(compactAssertion);
        replayGuard.consume(verified);
        // The proxied REQUIRES_NEW call has committed before returning here.
        return new AuthenticatedPrincipal(PrincipalType.WORKLOAD, verified.principalId().value(),
                AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, verified.kid().value(),
                AuthenticationStatus.VERIFIED);
    }
}
