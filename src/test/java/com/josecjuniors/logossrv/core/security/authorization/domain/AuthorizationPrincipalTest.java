package com.josecjuniors.logossrv.core.security.authorization.domain;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuthorizationPrincipalTest {
    @Test
    void acceptsStableWorkloadIdentityWithoutRewritingItsCase() {
        assertThat(new AuthorizationPrincipal(PrincipalType.WORKLOAD, "LifeOS").principalId()).isEqualTo("LifeOS");
    }

    @Test
    void rejectsNullBlankPaddedAndOversizedIdentities() {
        assertThatNullPointerException().isThrownBy(() -> new AuthorizationPrincipal(null, "lifeos"));
        assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationPrincipal(PrincipalType.WORKLOAD, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationPrincipal(PrincipalType.WORKLOAD, " "));
        assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationPrincipal(PrincipalType.WORKLOAD, " lifeos"));
        assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationPrincipal(PrincipalType.WORKLOAD, "lifeos "));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new AuthorizationPrincipal(PrincipalType.WORKLOAD, "x".repeat(129)));
    }
}
