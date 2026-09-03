package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Locale;
import java.util.Objects;

/** Namespaced, opaque identity supplied by an external consumer. */
public record ExternalSubjectReference(String namespace, String externalId) {

    public ExternalSubjectReference {
        namespace = normalizeNamespace(namespace);
        externalId = normalizeExternalId(externalId);
    }

    private static String normalizeNamespace(String value) {
        Objects.requireNonNull(value, "namespace");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("namespace must have at most 64 characters");
        }
        return normalized;
    }

    private static String normalizeExternalId(String value) {
        Objects.requireNonNull(value, "externalId");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("externalId must have at most 255 characters");
        }
        return normalized;
    }
}
