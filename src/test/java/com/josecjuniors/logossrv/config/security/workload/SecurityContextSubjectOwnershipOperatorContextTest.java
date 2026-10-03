package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.config.security.authorization.SecurityContextSubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationVerdict;
import com.josecjuniors.logossrv.core.security.authorization.application.exception.AuthorizationRegistryUnavailableException;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class SecurityContextSubjectOwnershipOperatorContextTest {
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final SecurityContextSubjectOwnershipOperatorContext context =
            new SecurityContextSubjectOwnershipOperatorContext(evaluator);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exactVerifiedWorkloadGrantReturnsStableAuditIdentity() {
        AuthenticatedPrincipal principal = principal("service-operator", "key-17");
        authenticate(principal);
        when(evaluator.evaluate(principal, AuthorizationOperation.SUBJECT_OWNERSHIP_MANAGE,
                Optional.empty(), Optional.of(new AuthorizationNamespace("lifeos"))))
                .thenReturn(AuthorizationVerdict.ALLOW);

        AuthorizedSubjectOwnershipOperator authorized = context.authorizeForNamespace("lifeos");

        assertThat(authorized.principalType()).isEqualTo(PrincipalType.WORKLOAD);
        assertThat(authorized.principalId()).isEqualTo("service-operator");
        assertThat(authorized.namespace()).isEqualTo("lifeos");
        assertThat(authorized.auditActorType()).isEqualTo("WORKLOAD_OPERATOR");
        verify(evaluator).evaluate(principal, AuthorizationOperation.SUBJECT_OWNERSHIP_MANAGE,
                Optional.empty(), Optional.of(new AuthorizationNamespace("lifeos")));
    }

    @Test
    void missingAuthenticationIsDeniedWithoutAuthorizationLookup() {
        assertDenied("lifeos");
        verifyNoInteractions(evaluator);
    }

    @Test
    void appUserAndUnsupportedAuthenticationTokensAreDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ordinary-app-user", "credentials", "ROLE_USER"));
        assertDenied("lifeos");

        authenticate(principal("service-operator", "key-17"));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal("service-operator", "key-17"), null, "ROLE_UNUSED"));
        assertDenied("lifeos");
        verifyNoInteractions(evaluator);
    }

    @Test
    void unauthenticatedAndNonCanonicalNamespaceAreDenied() {
        AuthenticatedPrincipal principal = principal("service-operator", "key-17");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
        assertDenied("lifeos");

        authenticate(principal);
        assertDenied("LifeOS");
        verifyNoInteractions(evaluator);
    }

    @Test
    void unverifiedOrMalformedPrincipalIsDenied() {
        AuthenticatedPrincipal unverified = mock(AuthenticatedPrincipal.class);
        when(unverified.principalType()).thenReturn(PrincipalType.WORKLOAD);
        when(unverified.principalId()).thenReturn("service-operator");
        when(unverified.authenticationStatus()).thenReturn(null);
        authenticate(unverified);
        assertDenied("lifeos");

        AuthenticatedPrincipal missingId = mock(AuthenticatedPrincipal.class);
        when(missingId.principalType()).thenReturn(PrincipalType.WORKLOAD);
        when(missingId.principalId()).thenReturn(" ");
        when(missingId.authenticationStatus()).thenReturn(AuthenticationStatus.VERIFIED);
        authenticate(missingId);
        assertDenied("lifeos");
        verifyNoInteractions(evaluator);
    }

    @Test
    void missingOrWrongNamespaceGrantIsDenied() {
        AuthenticatedPrincipal principal = principal("service-operator", "key-17");
        authenticate(principal);
        when(evaluator.evaluate(principal, AuthorizationOperation.SUBJECT_OWNERSHIP_MANAGE,
                Optional.empty(), Optional.of(new AuthorizationNamespace("lifeos"))))
                .thenReturn(AuthorizationVerdict.DENY);

        assertDenied("lifeos");

        verify(evaluator, never()).evaluate(any(AuthenticatedPrincipal.class),
                eq(AuthorizationOperation.PROGRESSION_EXECUTE), any(), any());
        verify(evaluator, never()).evaluate(any(AuthenticatedPrincipal.class),
                eq(AuthorizationOperation.SUBJECT_PROVISION), any(), any());
    }

    @Test
    void authorizationInfrastructureFailureIsDenied() {
        AuthenticatedPrincipal principal = principal("service-operator", "key-17");
        authenticate(principal);
        when(evaluator.evaluate(principal, AuthorizationOperation.SUBJECT_OWNERSHIP_MANAGE,
                Optional.empty(), Optional.of(new AuthorizationNamespace("lifeos"))))
                .thenThrow(new AuthorizationRegistryUnavailableException(new IllegalStateException("offline")));

        assertDenied("lifeos");
    }

    @Test
    void operatorValueCannotRepresentOtherPrincipalTypesOrUntrustedActorIds() {
        assertThatThrownBy(() -> new AuthorizedSubjectOwnershipOperator(null, "service", "lifeos"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD,
                " service ", "lifeos")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD,
                "service", "LifeOS")).isInstanceOf(IllegalArgumentException.class);
    }

    private static AuthenticatedPrincipal principal(String principalId, String credentialIdentity) {
        return new AuthenticatedPrincipal(PrincipalType.WORKLOAD, principalId,
                AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, credentialIdentity, AuthenticationStatus.VERIFIED);
    }

    private static void authenticate(AuthenticatedPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(WorkloadPrincipalAuthenticationToken.authenticated(principal));
    }

    private void assertDenied(String namespace) {
        assertThatThrownBy(() -> context.authorizeForNamespace(namespace))
                .isInstanceOf(AccessDeniedException.class);
    }
}
