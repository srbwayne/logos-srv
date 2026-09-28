package com.josecjuniors.logossrv.core.security.authorization.domain;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuthorizationGrantTest {
    private final AuthorizationPrincipal principal = new AuthorizationPrincipal(PrincipalType.WORKLOAD, "lifeos");
    private final AuthorizationSource source = new AuthorizationSource("lifeos");
    private final AuthorizationNamespace namespace = new AuthorizationNamespace("lifeos");

    @Test
    void acceptsEveryFrozenApplicabilityCombination() {
        assertThat(grant(AuthorizationOperation.PROGRESSION_EXECUTE, Optional.of(source), Optional.of(namespace))).isNotNull();
        assertThat(grant(AuthorizationOperation.PROGRESSION_EXECUTION_READ, Optional.of(source), Optional.empty())).isNotNull();
        assertThat(grant(AuthorizationOperation.PROGRESSION_HISTORY_READ, Optional.empty(), Optional.of(namespace))).isNotNull();
        assertThat(grant(AuthorizationOperation.SUBJECT_PROVISION, Optional.empty(), Optional.of(namespace))).isNotNull();
    }

    @Test
    void rejectsNullOptionalsAndEveryApplicabilityMismatch() {
        assertThatNullPointerException().isThrownBy(() -> new AuthorizationGrant(principal,
                AuthorizationOperation.PROGRESSION_EXECUTE, null, Optional.of(namespace)));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.empty(), Optional.of(namespace)));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(source), Optional.empty()));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                Optional.empty(), Optional.empty()));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                Optional.of(source), Optional.of(namespace)));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_HISTORY_READ,
                Optional.of(source), Optional.empty()));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.PROGRESSION_HISTORY_READ,
                Optional.empty(), Optional.empty()));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.SUBJECT_PROVISION,
                Optional.of(source), Optional.of(namespace)));
        assertThatIllegalArgumentException().isThrownBy(() -> grant(AuthorizationOperation.SUBJECT_PROVISION,
                Optional.empty(), Optional.empty()));
    }

    private AuthorizationGrant grant(AuthorizationOperation operation, Optional<AuthorizationSource> source,
                                     Optional<AuthorizationNamespace> namespace) {
        return new AuthorizationGrant(principal, operation, source, namespace);
    }
}
