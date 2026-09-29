package com.josecjuniors.logossrv.core.security.authorization.application;

import com.josecjuniors.logossrv.core.security.authentication.domain.*;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator.AuthorizationDecision;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import com.josecjuniors.logossrv.core.security.authorization.domain.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationEvaluatorTest {
    private final AuthorizationGrantStore store = mock(AuthorizationGrantStore.class);
    private final AuthorizationEvaluator evaluator = new AuthorizationEvaluator(store);
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "lifeos",
            AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "credential", AuthenticationStatus.VERIFIED);
    private final AuthorizationGrant canonical = new AuthorizationGrant(
            new AuthorizationPrincipal(PrincipalType.WORKLOAD, "lifeos"), AuthorizationOperation.PROGRESSION_EXECUTE,
            Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("lifeos")));

    @Test void exactGrantAllows() {
        when(store.findExact(canonical)).thenReturn(Optional.of(canonical));
        assertThat(evaluate(principal, canonical)).isEqualTo(AuthorizationDecision.ALLOW);
    }
    @Test void absentGrantDenies() {
        when(store.findExact(canonical)).thenReturn(Optional.empty());
        assertThat(evaluate(principal, canonical)).isEqualTo(AuthorizationDecision.DENY);
    }
    @Test void mismatchingPrincipalOperationSourceAndNamespaceDeny() {
        when(store.findExact(any())).thenReturn(Optional.empty());
        assertThat(evaluate(new AuthenticatedPrincipal(PrincipalType.WORKLOAD, "other",
                AuthenticationMethod.ASYMMETRIC_SIGNED_ASSERTION, "credential", AuthenticationStatus.VERIFIED), canonical))
                .isEqualTo(AuthorizationDecision.DENY);
        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                Optional.of(new AuthorizationSource("lifeos")), Optional.empty())).isEqualTo(AuthorizationDecision.DENY);
        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("other")), Optional.of(new AuthorizationNamespace("lifeos"))))
                .isEqualTo(AuthorizationDecision.DENY);
        assertThat(evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(new AuthorizationSource("lifeos")), Optional.of(new AuthorizationNamespace("other"))))
                .isEqualTo(AuthorizationDecision.DENY);
    }
    @Test void nonVerifiedStatusIsStructurallyUnreachableWithCurrentEnum() {
        assertThat(AuthenticationStatus.values()).containsExactly(AuthenticationStatus.VERIFIED);
    }
    private AuthorizationDecision evaluate(AuthenticatedPrincipal p, AuthorizationGrant g) {
        return evaluator.evaluate(p, g.operation(), g.source(), g.namespace());
    }
}
