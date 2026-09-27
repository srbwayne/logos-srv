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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkloadBearerAuthenticationFilterTest {
    private static final String PATH = "/__test/workload/only";
    private static final String TOKEN = "synthetic-compact-assertion";
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
            AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "kid-a", AuthenticationStatus.VERIFIED);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validWorkloadAuthenticationSetsFreshContextAndDoesNotRetainAssertion() throws Exception {
        AtomicInteger authenticationCalls = new AtomicInteger();
        WorkloadBearerAuthenticationFilter filter = filter(assertion -> {
            authenticationCalls.incrementAndGet();
            return principal;
        });
        MockHttpServletRequest request = matchedRequest("bEaReR " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication[] downstreamAuthentication = new Authentication[1];

        filter.doFilter(request, response, (req, res) ->
                downstreamAuthentication[0] = SecurityContextHolder.getContext().getAuthentication());

        assertThat(authenticationCalls).hasValue(1);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(downstreamAuthentication[0]).isInstanceOf(WorkloadPrincipalAuthenticationToken.class);
        WorkloadPrincipalAuthenticationToken authentication =
                (WorkloadPrincipalAuthenticationToken) downstreamAuthentication[0];
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(principal);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getDetails()).isNull();
        assertThat(authentication.toString()).doesNotContain(TOKEN);
    }

    @Test
    void malformedOrAmbiguousAuthorizationDoesNotCallManager() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        WorkloadBearerAuthenticationFilter filter = filter(assertion -> {
            calls.incrementAndGet();
            return principal;
        });
        List<String> invalidHeaders = List.of("Basic xyz", "Bearer", "Bearer ", " Bearer " + TOKEN,
                "Bearer  " + TOKEN, "Bearer " + TOKEN + " ", "Bearer a,b", "Bearer token\twith-tab",
                "Bearer " + "a".repeat(8193));
        for (String header : invalidHeaders) {
            MockHttpServletResponse response = invoke(filter, matchedRequest(header));
            assertThat(response.getStatus()).as("header %s", header.length() > 100 ? "oversized" : header)
                    .isEqualTo(401);
        }
        MockHttpServletRequest multiple = matchedRequest(TOKEN);
        multiple.addHeader("Authorization", "Bearer " + TOKEN);
        multiple.addHeader("Authorization", "Bearer " + TOKEN);
        assertThat(invoke(filter, multiple).getStatus()).isEqualTo(401);
        assertThat(calls).hasValue(0);
    }

    @Test
    void matchedRouteCannotReusePreexistingHumanContext() throws Exception {
        var human = UsernamePasswordAuthenticationToken.authenticated("human@example.test", "human-secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(human);
        AtomicInteger downstreamCalls = new AtomicInteger();
        WorkloadBearerAuthenticationFilter filter = filter(assertion -> principal);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", PATH), response,
                (req, res) -> downstreamCalls.incrementAndGet());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(downstreamCalls).hasValue(0);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unmatchedRouteDoesNotInspectBearerOrAlterHumanContext() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        WorkloadBearerAuthenticationFilter filter = filter(assertion -> {
            calls.incrementAndGet();
            return principal;
        });
        var human = UsernamePasswordAuthenticationToken.authenticated("human", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(human);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ordinary/user/route");
        request.addHeader("Authorization", "malformed and ignored");
        AtomicInteger downstreamCalls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            downstreamCalls.incrementAndGet();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(human);
        });

        assertThat(calls).hasValue(0);
        assertThat(downstreamCalls).hasValue(1);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(human);
    }

    @Test
    void callerFailuresReturn401AndInfrastructureFailuresReturn503WithoutContinuing() throws Exception {
        assertFailure(new WorkloadAssertionVerificationException(WorkloadAssertionFailureReason.SIGNATURE_INVALID), 401);
        assertFailure(new WorkloadAssertionReplayException(), 401);
        assertFailure(new WorkloadReplayStoreUnavailableException(), 503);
        assertFailure(new WorkloadTrustRegistryIntegrityException("do not expose this"), 503);
    }

    private void assertFailure(RuntimeException coreFailure, int expectedStatus) throws Exception {
        WorkloadBearerAuthenticationFilter filter = filter(assertion -> {
            throw coreFailure;
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger downstreamCalls = new AtomicInteger();
        filter.doFilter(matchedRequest("Bearer " + TOKEN), response, (req, res) -> downstreamCalls.incrementAndGet());
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getContentAsString()).doesNotContain(TOKEN, "do not expose this", "SIGNATURE_INVALID");
        assertThat(downstreamCalls).hasValue(0);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private WorkloadBearerAuthenticationFilter filter(java.util.function.Function<String, AuthenticatedPrincipal> authenticators) {
        WorkloadAuthenticationService service = mock(WorkloadAuthenticationService.class);
        when(service.authenticate(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation ->
                authenticators.apply(invocation.getArgument(0)));
        var manager = new ProviderManager(List.of(new WorkloadAuthenticationProvider(service)));
        return new WorkloadBearerAuthenticationFilter(manager,
                request -> request.getRequestURI().startsWith("/__test/workload/"),
                new WorkloadAuthenticationFailureResponder());
    }

    private static MockHttpServletRequest matchedRequest(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);
        request.addHeader("Authorization", authorization);
        return request;
    }

    private static MockHttpServletResponse invoke(WorkloadBearerAuthenticationFilter filter,
                                                  MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("invalid bearer must not reach downstream chain");
        });
        return response;
    }
}
