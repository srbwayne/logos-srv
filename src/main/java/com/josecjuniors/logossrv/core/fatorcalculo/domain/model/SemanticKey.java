package com.josecjuniors.logossrv.core.fatorcalculo.domain.model;

import java.util.Locale;

public record SemanticKey(String value) {
    private static final String PATTERN = "^[a-z][a-z0-9_]{0,63}$";

    public SemanticKey {
        if (value == null) {
            throw new IllegalArgumentException("semanticKey é obrigatório.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!value.matches(PATTERN)) {
            throw new IllegalArgumentException("semanticKey deve usar snake_case ASCII com até 64 caracteres.");
        }
    }

    public static SemanticKey of(String value) {
        return new SemanticKey(value);
    }
}
