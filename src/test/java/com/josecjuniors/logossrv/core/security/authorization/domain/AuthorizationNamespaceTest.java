package com.josecjuniors.logossrv.core.security.authorization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuthorizationNamespaceTest {
    @Test
    void canonicalizesSupportedIdentifiers() {
        assertThat(new AuthorizationNamespace(" LifeOS ").value()).isEqualTo("lifeos");
        assertThat(new AuthorizationNamespace("logos-native").value()).isEqualTo("logos-native");
        assertThat(new AuthorizationNamespace("logos.activity").value()).isEqualTo("logos.activity");
        assertThat(new AuthorizationNamespace("logos").value()).isEqualTo("logos");
        assertThat(new AuthorizationNamespace("noema").value()).isEqualTo("noema");
        assertThat(new AuthorizationNamespace("a").value()).isEqualTo("a");
        assertThat(new AuthorizationNamespace("a".repeat(64)).value()).hasSize(64);
    }

    @Test
    void rejectsNullBlankOverlengthWildcardAndOutsideGrammar() {
        assertThatNullPointerException().isThrownBy(() -> new AuthorizationNamespace(null));
        for (String invalid : new String[]{"", " ", "*", "abc*", "*abc", "a b", "a:b", "é", "a".repeat(65)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationNamespace(invalid));
        }
    }
}
