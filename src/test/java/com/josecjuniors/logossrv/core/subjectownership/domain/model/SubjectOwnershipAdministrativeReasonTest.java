package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAdministrativeReasonTest {
    @Test void trimsRequiredReason() {
        assertThat(new SubjectOwnershipAdministrativeReason("  approved suspension  ").value())
                .isEqualTo("approved suspension");
    }

    @Test void rejectsMissingBlankAndTooLongReason() {
        assertThatThrownBy(() -> new SubjectOwnershipAdministrativeReason(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubjectOwnershipAdministrativeReason(" \t ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubjectOwnershipAdministrativeReason("x".repeat(513)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new SubjectOwnershipAdministrativeReason("x".repeat(512)).value()).hasSize(512);
    }
}
