package com.josecjuniors.logossrv.core.progression.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalProgressionConfigurationReferenceTest {
    @Test
    void normalizesKeyAndPreservesRevision() {
        var reference = new ExternalProgressionConfigurationReference(" Daily-Reading ", 3);

        assertThat(reference.key()).isEqualTo("daily-reading");
        assertThat(reference.revision()).isEqualTo(3);
    }

    @Test
    void acceptsKeyOnlyForCurrentResolution() {
        assertThat(new ExternalProgressionConfigurationReference("reading", null).revision()).isNull();
    }

    @Test
    void rejectsMissingOrOversizedKey() {
        assertThatThrownBy(() -> new ExternalProgressionConfigurationReference(null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalProgressionConfigurationReference(" ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalProgressionConfigurationReference("x".repeat(256), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveRevision() {
        assertThatThrownBy(() -> new ExternalProgressionConfigurationReference("reading", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalProgressionConfigurationReference("reading", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
