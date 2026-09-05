package com.josecjuniors.logossrv.core.progression.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ProgressionExecutionIdentityTest {

    @Test
    void normalizesSourceAndTrimsKeyWithoutChangingItsCase() {
        var identity = new ProgressionExecutionIdentity(" LifeOS ", " ABC123 ");

        assertThat(identity.source()).isEqualTo("lifeos");
        assertThat(identity.idempotencyKey()).isEqualTo("ABC123");
    }

    @Test
    void keepsIdempotencyKeyCaseSensitive() {
        assertThat(new ProgressionExecutionIdentity("lifeos", "ABC123"))
                .isNotEqualTo(new ProgressionExecutionIdentity("lifeos", "abc123"));
    }

    @Test
    void rejectsMissingAndOversizedValues() {
        assertThatNullPointerException().isThrownBy(() -> new ProgressionExecutionIdentity(null, "key"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProgressionExecutionIdentity(" ", "key"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProgressionExecutionIdentity("lifeos", " "));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProgressionExecutionIdentity("x".repeat(65), "key"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProgressionExecutionIdentity("lifeos", "x".repeat(256)));
    }
}
