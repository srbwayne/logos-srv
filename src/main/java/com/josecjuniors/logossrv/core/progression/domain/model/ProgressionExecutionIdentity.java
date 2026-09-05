package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Locale;
import java.util.Objects;

public record ProgressionExecutionIdentity(String source, String idempotencyKey) {
    public ProgressionExecutionIdentity {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        source = source.trim().toLowerCase(Locale.ROOT);
        idempotencyKey = idempotencyKey.trim();
        if (source.isEmpty() || source.length() > 64) throw new IllegalArgumentException("source must have 1 to 64 characters");
        if (idempotencyKey.isEmpty() || idempotencyKey.length() > 255) throw new IllegalArgumentException("idempotency key must have 1 to 255 characters");
    }
}
