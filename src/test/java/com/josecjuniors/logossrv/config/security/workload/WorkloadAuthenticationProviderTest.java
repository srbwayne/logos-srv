package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionFailureReason;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionVerificationException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadAssertionReplayException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadReplayStoreUnavailableException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkloadAuthenticationProviderTest {
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
            AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "kid-verified", AuthenticationStatus.VERIFIED);

    @Test
    void supportsOnlyTheUnauthenticatedWorkloadRequestToken() {
        WorkloadAuthenticationProvider provider = new WorkloadAuthenticationProvider(mock(WorkloadAuthenticationService.class));
        assertThat(provider.supports(WorkloadAssertionAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(WorkloadPrincipalAuthenticationToken.class)).isFalse();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
        assertThat(provider.supports(TestingAuthenticationToken.class)).isFalse();
    }

    @Test
    void successfulAuthenticationErasesCredentialsAndReturnsOnlyNormalizedIdentity() {
        WorkloadAuthenticationService service = mock(WorkloadAuthenticationService.class);
        when(service.authenticate("signed-assertion")).thenReturn(principal);
        WorkloadAuthenticationProvider provider = new WorkloadAuthenticationProvider(service);

        var request = WorkloadAssertionAuthenticationToken.unauthenticated("signed-assertion");
        assertThatThrownBy(() -> request.setAuthenticated(true)).isInstanceOf(IllegalArgumentException.class);
        var result = (WorkloadPrincipalAuthenticationToken) provider.authenticate(request);

        assertThat(request.isAuthenticated()).isFalse();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(principal);
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getAuthorities()).isEmpty();
        assertThat(result.getDetails()).isNull();
        assertThat(result.getName()).isEqualTo("lifeos");
        verify(service).authenticate("signed-assertion");
    }

    @Test
    void callerFailuresAreMappedToGenericBadCredentials() {
        assertCallerFailure(new WorkloadAssertionVerificationException(WorkloadAssertionFailureReason.SIGNATURE_INVALID));
        assertCallerFailure(new WorkloadAssertionReplayException());
    }

    @Test
    void trustAndReplayInfrastructureFailuresAreMappedToGenericServiceFailure() {
        assertServiceFailure(new WorkloadReplayStoreUnavailableException());
        assertServiceFailure(new WorkloadTrustRegistryIntegrityException("synthetic internal detail"));
    }

    @Test
    void arbitraryAuthenticationIsNotAcceptedByProvider() {
        WorkloadAuthenticationProvider provider = new WorkloadAuthenticationProvider(mock(WorkloadAuthenticationService.class));
        assertThatThrownBy(() -> provider.authenticate(new TestingAuthenticationToken("human", "password")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unauthorized");
    }

    private void assertCallerFailure(RuntimeException coreFailure) {
        WorkloadAuthenticationService service = mock(WorkloadAuthenticationService.class);
        when(service.authenticate("signed-assertion")).thenThrow(coreFailure);
        WorkloadAuthenticationProvider provider = new WorkloadAuthenticationProvider(service);
        assertThatThrownBy(() -> provider.authenticate(WorkloadAssertionAuthenticationToken.unauthenticated("signed-assertion")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unauthorized")
                .hasNoCause();
    }

    private void assertServiceFailure(RuntimeException coreFailure) {
        WorkloadAuthenticationService service = mock(WorkloadAuthenticationService.class);
        when(service.authenticate("signed-assertion")).thenThrow(coreFailure);
        WorkloadAuthenticationProvider provider = new WorkloadAuthenticationProvider(service);
        assertThatThrownBy(() -> provider.authenticate(WorkloadAssertionAuthenticationToken.unauthenticated("signed-assertion")))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessage("Authentication service unavailable")
                .hasNoCause();
    }
}
