package com.josecjuniors.logossrv.core.atributo.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record AtributoSemanticKey(String value) {
    private static final Pattern VALID = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    public AtributoSemanticKey {
        Objects.requireNonNull(value, "semanticKey must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("semanticKey must be lowercase ASCII snake_case (1-64 characters)");
        }
    }

    public static AtributoSemanticKey of(String value) {
        return new AtributoSemanticKey(value);
    }
}
