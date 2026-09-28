package com.josecjuniors.logossrv.core.security.authorization.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record AuthorizationNamespace(String value) {
    private static final Pattern GRAMMAR = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    public AuthorizationNamespace {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!GRAMMAR.matcher(value).matches()) {
            throw new IllegalArgumentException("namespace must match [a-z0-9][a-z0-9._-]{0,63}");
        }
    }
}
