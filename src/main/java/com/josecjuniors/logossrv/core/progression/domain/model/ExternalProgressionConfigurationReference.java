package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Locale;

public record ExternalProgressionConfigurationReference(String key, Integer revision) {
    public ExternalProgressionConfigurationReference {
        key = normalizeKey(key);
        if (revision != null && revision < 1) throw new IllegalArgumentException("configuration revision must be positive");
    }

    public static String normalizeKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("configuration key is required");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) throw new IllegalArgumentException("configuration key exceeds 255 characters");
        return normalized;
    }
}
