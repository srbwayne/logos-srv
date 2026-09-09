package com.josecjuniors.logossrv.core.fatorcalculo.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticKeyTest {
    @Test
    void canonicalizesTrimAndCase() {
        assertThat(SemanticKey.of("  Pages_Read  ").value()).isEqualTo("pages_read");
    }

    @Test
    void acceptsMaximumLength() {
        assertThat(SemanticKey.of("a" + "b".repeat(63)).value()).hasSize(64);
    }

    @Test
    void rejectsInvalidKeys() {
        assertThatThrownBy(() -> SemanticKey.of("pages-read")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticKey.of("páginas_lidas")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticKey.of("123_pages")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticKey.of("a" + "b".repeat(64))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignedSemanticKeyCannotChange() {
        var fator = new FatorCalculo(FatorCalculoId.generate(), "Practice", "min", null, "minutes_practiced");
        assertThatThrownBy(() -> fator.assignSemanticKey("pieces_completed"))
                .isInstanceOf(IllegalStateException.class);
        fator.assignSemanticKey(" MINUTES_PRACTICED ");
        assertThat(fator.getSemanticKey()).isEqualTo("minutes_practiced");
    }
}
