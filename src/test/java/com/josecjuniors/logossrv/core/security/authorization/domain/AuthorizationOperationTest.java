package com.josecjuniors.logossrv.core.security.authorization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationOperationTest {
    @Test
    void exposesOnlyFrozenOperationsAndTheirApplicability() {
        assertThat(AuthorizationOperation.values()).containsExactly(
                AuthorizationOperation.PROGRESSION_EXECUTE,
                AuthorizationOperation.PROGRESSION_EXECUTION_READ,
                AuthorizationOperation.PROGRESSION_HISTORY_READ,
                AuthorizationOperation.SUBJECT_PROVISION);
        assertThat(AuthorizationOperation.PROGRESSION_EXECUTE.requiresSource()).isTrue();
        assertThat(AuthorizationOperation.PROGRESSION_EXECUTE.requiresNamespace()).isTrue();
        assertThat(AuthorizationOperation.PROGRESSION_EXECUTION_READ.requiresSource()).isTrue();
        assertThat(AuthorizationOperation.PROGRESSION_EXECUTION_READ.requiresNamespace()).isFalse();
        assertThat(AuthorizationOperation.PROGRESSION_HISTORY_READ.requiresSource()).isFalse();
        assertThat(AuthorizationOperation.PROGRESSION_HISTORY_READ.requiresNamespace()).isTrue();
        assertThat(AuthorizationOperation.SUBJECT_PROVISION.requiresSource()).isFalse();
        assertThat(AuthorizationOperation.SUBJECT_PROVISION.requiresNamespace()).isTrue();
    }
}
