package com.josecjuniors.logossrv.core.security.workload.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadTrustValueTypesTest {
    @Test
    void issuerIsValidatedWithoutChangingCase() {
        assertThat(new WorkloadIssuer("URN:Akume:Workload:LifeOS").value())
                .isEqualTo("URN:Akume:Workload:LifeOS");
        assertThatThrownBy(() -> new WorkloadIssuer(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkloadIssuer("i".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void principalIdIsNonblankAndBounded() {
        assertThat(new WorkloadPrincipalId("lifeos").value()).isEqualTo("lifeos");
        assertThatThrownBy(() -> new WorkloadPrincipalId("\t")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkloadPrincipalId("p".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keyIdUsesExactAsciiProfileWithoutNormalization() {
        for (String valid : new String[]{"A", "kid-A.2", "0_key-x", "x".repeat(64)}) {
            assertThat(new WorkloadKeyId(valid).value()).isEqualTo(valid);
        }
        for (String invalid : new String[]{"", "-starts-invalid", "has space", "ümlaut", "x".repeat(65)}) {
            assertThatThrownBy(() -> new WorkloadKeyId(invalid))
                    .as("kid %s", invalid).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void profileEnumsExposeOnlyFrozenPersistedValues() {
        assertThat(WorkloadSignatureAlgorithm.values()).containsExactly(WorkloadSignatureAlgorithm.ES256);
        assertThat(WorkloadPrincipalLifecycle.values()).containsExactly(
                WorkloadPrincipalLifecycle.ACTIVE, WorkloadPrincipalLifecycle.DISABLED,
                WorkloadPrincipalLifecycle.REVOKED);
        assertThat(WorkloadCredentialLifecycle.values()).containsExactly(
                WorkloadCredentialLifecycle.PENDING, WorkloadCredentialLifecycle.ACTIVE,
                WorkloadCredentialLifecycle.REVOKED, WorkloadCredentialLifecycle.RETIRED);
    }
}
