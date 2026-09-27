package com.josecjuniors.logossrv.core.security.workload.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadReplayStoreUnavailableException;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkloadAuthenticationServiceTest {
    private static final VerifiedWorkloadAssertion VERIFIED = new VerifiedWorkloadAssertion(
            new WorkloadIssuer("urn:akume:workload-issuer:lifeos"), new WorkloadPrincipalId("lifeos"),
            new WorkloadKeyId("kid-a"), UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:01:00Z"));

    @Test
    void replayInfrastructureFailureFailsClosedWithoutReturningPrincipal() {
        WorkloadAssertionVerifier verifier = mock(WorkloadAssertionVerifier.class);
        WorkloadAssertionReplayGuard replayGuard = mock(WorkloadAssertionReplayGuard.class);
        when(verifier.verify("compact")).thenReturn(VERIFIED);
        doThrow(new WorkloadReplayStoreUnavailableException()).when(replayGuard).consume(VERIFIED);
        WorkloadAuthenticationService service = new WorkloadAuthenticationService(verifier, replayGuard);

        assertThatThrownBy(() -> service.authenticate("compact"))
                .isInstanceOf(WorkloadReplayStoreUnavailableException.class);
        verify(replayGuard).consume(VERIFIED);
    }

    @Test
    void verificationFailureDoesNotInvokeReplayGuard() {
        WorkloadAssertionVerifier verifier = mock(WorkloadAssertionVerifier.class);
        WorkloadAssertionReplayGuard replayGuard = mock(WorkloadAssertionReplayGuard.class);
        RuntimeException verificationFailure = new WorkloadAssertionVerificationException(
                WorkloadAssertionFailureReason.SIGNATURE_INVALID);
        when(verifier.verify("invalid")).thenThrow(verificationFailure);
        WorkloadAuthenticationService service = new WorkloadAuthenticationService(verifier, replayGuard);

        assertThatThrownBy(() -> service.authenticate("invalid")).isSameAs(verificationFailure);
        verify(replayGuard, never()).consume(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void normalizedPrincipalContainsOnlyTheAuthenticationHandoffFields() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
                AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "kid-a", AuthenticationStatus.VERIFIED);

        assertThat(principal.getClass().getRecordComponents()).extracting("name")
                .containsExactly("principalType", "principalId", "authenticationMethod",
                        "credentialIdentity", "authenticationStatus");
        assertThat(principal.principalId()).isEqualTo("lifeos");
    }
}
