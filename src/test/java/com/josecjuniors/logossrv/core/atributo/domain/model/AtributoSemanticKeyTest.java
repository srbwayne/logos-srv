package com.josecjuniors.logossrv.core.atributo.domain.model;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoSemanticKeyJaAtribuidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtributoSemanticKeyTest {
    @Test
    void normalizesTrimmedAndUppercaseValues() {
        assertThat(AtributoSemanticKey.of(" Knowledge ").value()).isEqualTo("knowledge");
        assertThat(AtributoSemanticKey.of("READING_KNOWLEDGE").value()).isEqualTo("reading_knowledge");
    }

    @Test
    void rejectsInvalidValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> AtributoSemanticKey.of("Knowledge Space"));
        assertThatIllegalArgumentException().isThrownBy(() -> AtributoSemanticKey.of("conhecimento-é"));
        assertThatIllegalArgumentException().isThrownBy(() -> AtributoSemanticKey.of("123knowledge"));
        assertThatIllegalArgumentException().isThrownBy(() -> AtributoSemanticKey.of(""));
    }

    @Test
    void semanticIdentityIsImmutableAfterAssignment() {
        Atributo atributo = new Atributo(new AtributoId(), "Conhecimento", null);
        atributo.assignSemanticKey(" Knowledge ");
        atributo.assignSemanticKey("knowledge");
        assertThat(atributo.getSemanticKey()).isEqualTo("knowledge");
        assertThatThrownBy(() -> atributo.assignSemanticKey("intelligence"))
                .isInstanceOf(AtributoSemanticKeyJaAtribuidaException.class);
    }
}
