package com.josecjuniors.logossrv.core.progression.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ExternalSubjectReferenceTest {

    @Test
    void normalizaNamespaceEExternalId() {
        var reference = new ExternalSubjectReference(" LifeOS ", " ABC123 ");

        assertThat(reference.namespace()).isEqualTo("lifeos");
        assertThat(reference.externalId()).isEqualTo("ABC123");
    }

    @Test
    void preservaCaseDoExternalId() {
        assertThat(new ExternalSubjectReference("lifeos", "ABC123"))
                .isNotEqualTo(new ExternalSubjectReference("lifeos", "abc123"));
    }

    @Test
    void rejeitaValoresNulosOuVazios() {
        assertThatNullPointerException().isThrownBy(() -> new ExternalSubjectReference(null, "id"));
        assertThatNullPointerException().isThrownBy(() -> new ExternalSubjectReference("lifeos", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExternalSubjectReference(" ", "id"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExternalSubjectReference("lifeos", " "));
    }

    @Test
    void rejeitaValoresAcimaDosLimitesAprovados() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExternalSubjectReference("a".repeat(65), "id"));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExternalSubjectReference("lifeos", "a".repeat(256)));
    }
}
