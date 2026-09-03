package com.josecjuniors.logossrv.core.progression.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SubjectIdTest {

    @Test
    void possuiSemanticaDeValorERepresentaIdentidadeOpaca() {
        UUID value = UUID.randomUUID();

        assertThat(new SubjectId(value)).isEqualTo(new SubjectId(value));
        assertThat(new SubjectId(value).value()).isEqualTo(value);
    }

    @Test
    void rejeitaValorNulo() {
        assertThatNullPointerException().isThrownBy(() -> new SubjectId(null));
    }
}
