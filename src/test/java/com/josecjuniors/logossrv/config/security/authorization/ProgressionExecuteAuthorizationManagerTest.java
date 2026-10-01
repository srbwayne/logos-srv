package com.josecjuniors.logossrv.config.security.authorization;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionExecutionRequest;
import com.josecjuniors.logossrv.config.security.workload.WorkloadPrincipalAuthenticationToken;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationVerdict;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgressionExecuteAuthorizationManagerTest {
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final ProgressionExecuteAuthorizationManager manager = new ProgressionExecuteAuthorizationManager(evaluator);
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
            AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "credential", AuthenticationStatus.VERIFIED);

    @Test
    void acceptsOnlyAuthenticatedWorkloadTokenAndPassesExactAuthorizationDimensions() throws Exception {
        WorkloadPrincipalAuthenticationToken token = workloadToken(principal, true);
        when(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("lifeos"))))
                .thenReturn(AuthorizationVerdict.ALLOW);

        AuthorizationDecision decision = authorize(token, request(" LIFEOS ", " LifeOS "));

        assertThat(decision.isGranted()).isTrue();
        verify(evaluator).evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("lifeos")));
    }

    @Test
    void mapsCoreDenyToSpringDeny() throws Exception {
        when(evaluator.evaluate(any(), any(), any(), any())).thenReturn(AuthorizationVerdict.DENY);

        assertThat(authorize(workloadToken(principal, true), request("lifeos", "lifeos")).isGranted()).isFalse();
    }

    @Test
    void rejectsMissingUnauthenticatedAndNonWorkloadAuthentication() throws Exception {
        assertDenied(null, request("lifeos", "lifeos"));
        assertDenied(UsernamePasswordAuthenticationToken.unauthenticated("human", "password"), request("lifeos", "lifeos"));
        assertDenied(workloadToken(principal, false), request("lifeos", "lifeos"));

        WorkloadPrincipalAuthenticationToken malformedToken = mock(WorkloadPrincipalAuthenticationToken.class);
        when(malformedToken.isAuthenticated()).thenReturn(true);
        when(malformedToken.getPrincipal()).thenReturn(null);
        assertDenied(malformedToken, request("lifeos", "lifeos"));
        verify(evaluator, never()).evaluate(any(), any(), any(), any());
    }

    @Test
    void missingRequestDimensionsAndInvalidGrammarDenyWithoutEvaluatorCall() throws Exception {
        WorkloadPrincipalAuthenticationToken token = workloadToken(principal, true);
        for (ProgressionExecutionRequest request : Arrays.asList(
                null,
                new ProgressionExecutionRequest(null, new ProgressionExecutionRequest.ExecutionIdentity("lifeos", "key"), null, null),
                new ProgressionExecutionRequest(new ProgressionExecutionRequest.SubjectReference(null, "subject"),
                        new ProgressionExecutionRequest.ExecutionIdentity("lifeos", "key"), null, null),
                new ProgressionExecutionRequest(new ProgressionExecutionRequest.SubjectReference("lifeos", "subject"),
                        new ProgressionExecutionRequest.ExecutionIdentity(null, "key"), null, null),
                request("bad source", "lifeos"))) {
            assertDenied(token, request);
        }
        verify(evaluator, never()).evaluate(any(), any(), any(), any());
    }

    private void assertDenied(Authentication authentication, ProgressionExecutionRequest request) throws Exception {
        assertThat(authorize(authentication, request).isGranted()).isFalse();
    }

    private AuthorizationDecision authorize(Authentication authentication, ProgressionExecutionRequest request) throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = Target.class.getMethod("invoke", ProgressionExecutionRequest.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{request});
        return manager.check(() -> authentication, invocation);
    }

    private static WorkloadPrincipalAuthenticationToken workloadToken(AuthenticatedPrincipal principal, boolean authenticated) {
        WorkloadPrincipalAuthenticationToken token = mock(WorkloadPrincipalAuthenticationToken.class);
        when(token.isAuthenticated()).thenReturn(authenticated);
        when(token.getPrincipal()).thenReturn(principal);
        return token;
    }

    private static ProgressionExecutionRequest request(String source, String namespace) {
        return new ProgressionExecutionRequest(
                new ProgressionExecutionRequest.SubjectReference(namespace, "subject"),
                new ProgressionExecutionRequest.ExecutionIdentity(source, "key"), null, null);
    }

    public static class Target {
        public void invoke(ProgressionExecutionRequest request) { }
    }
}
