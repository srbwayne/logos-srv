package com.josecjuniors.logossrv.core.security.authorization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class AuthorizationSourceTest {
    @Test
    void canonicalizesSupportedIdentifiers() {
        assertThat(new AuthorizationSource(" LifeOS ").value()).isEqualTo("lifeos");
        assertThat(new AuthorizationSource("logos-native").value()).isEqualTo("logos-native");
        assertThat(new AuthorizationSource("logos.activity").value()).isEqualTo("logos.activity");
        assertThat(new AuthorizationSource("logos").value()).isEqualTo("logos");
        assertThat(new AuthorizationSource("noema").value()).isEqualTo("noema");
        assertThat(new AuthorizationSource("a").value()).isEqualTo("a");
        assertThat(new AuthorizationSource("a".repeat(64)).value()).hasSize(64);
    }

    @Test
    void rejectsNullBlankOverlengthWildcardAndOutsideGrammar() {
        assertThatNullPointerException().isThrownBy(() -> new AuthorizationSource(null));
        for (String invalid : new String[]{"", " ", "*", "abc*", "*abc", "a b", "a:b", "é", "a".repeat(65)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new AuthorizationSource(invalid));
        }
    }
}
