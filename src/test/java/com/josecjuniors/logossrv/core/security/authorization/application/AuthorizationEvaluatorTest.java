package com.josecjuniors.logossrv.core.security.authorization.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationMethod;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationEvaluatorTest {
    private final AuthorizationGrantStore store = mock(AuthorizationGrantStore.class);
    private final AuthorizationEvaluator evaluator = new AuthorizationEvaluator(store);
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
            AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "credential", AuthenticationStatus.VERIFIED);
    private final AuthorizationGrant grant = new AuthorizationGrant(
            new AuthorizationPrincipal(PrincipalType.WORKLOAD, "lifeos"), AuthorizationOperation.PROGRESSION_EXECUTE,
            Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("lifeos")));

    @Test
    void exactGrantAllows() {
        when(store.findExact(grant)).thenReturn(Optional.of(grant));

        assertThat(evaluate()).isEqualTo(AuthorizationVerdict.ALLOW);
        verify(store).findExact(grant);
    }

    @Test
    void missingExactGrantDenies() {
        when(store.findExact(grant)).thenReturn(Optional.empty());

        assertThat(evaluate()).isEqualTo(AuthorizationVerdict.DENY);
    }

    @Test
    void stablePrincipalOperationSourceAndNamespaceArePartOfExactLookup() {
        when(store.findExact(grant)).thenReturn(Optional.empty());

        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                Optional.of(new AuthorizationSource("lifeos")), Optional.empty())).isEqualTo(AuthorizationVerdict.DENY);
        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("other")), Optional.of(new AuthorizationNamespace("lifeos"))))
                .isEqualTo(AuthorizationVerdict.DENY);
        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("other"))))
                .isEqualTo(AuthorizationVerdict.DENY);
    }

    @Test
    void currentIdentityEnumsContainOnlyVerifiedWorkload() {
        assertThat(PrincipalType.values()).containsExactly(PrincipalType.WORKLOAD);
        assertThat(AuthenticationStatus.values()).containsExactly(AuthenticationStatus.VERIFIED);
    }

    private AuthorizationVerdict evaluate() {
        return evaluator.evaluate(principal, grant.operation(), grant.source(), grant.namespace());
    }
}
