package com.josecjuniors.logossrv.core.security.workload.admin.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrustAdministrationActorTest {
    @Test
    void acceptsCanonicalActorTypesAndPreservesActorId() {
        var operator = new TrustAdministrationActor(TrustAdministrationActorType.OPERATOR, " operator ");
        var system = new TrustAdministrationActor(TrustAdministrationActorType.SYSTEM, "bootstrap-tool");

        assertThat(operator.actorType()).isEqualTo(TrustAdministrationActorType.OPERATOR);
        assertThat(operator.actorId()).isEqualTo(" operator ");
        assertThat(system.actorType()).isEqualTo(TrustAdministrationActorType.SYSTEM);
    }

    @Test
    void rejectsMissingBlankOrOversizedActor() {
        assertThatThrownBy(() -> new TrustAdministrationActor(null, "operator"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TrustAdministrationActor(TrustAdministrationActorType.OPERATOR, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustAdministrationActor(TrustAdministrationActorType.OPERATOR, " \t "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustAdministrationActor(TrustAdministrationActorType.SYSTEM, "x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new TrustAdministrationActor(TrustAdministrationActorType.SYSTEM, "x".repeat(128)).actorId())
                .hasSize(128);
    }

    @Test
    void reasonIsRequiredBoundedAndPreserved() {
        assertThat(new TrustAdministrationReason("  incident response  ").value()).isEqualTo("  incident response  ");
        assertThat(new TrustAdministrationReason("r".repeat(512)).value()).hasSize(512);
        assertThatThrownBy(() -> new TrustAdministrationReason(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustAdministrationReason(" \n ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustAdministrationReason("r".repeat(513)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
