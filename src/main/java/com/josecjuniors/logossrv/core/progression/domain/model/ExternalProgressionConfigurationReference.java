package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Locale;

public record ExternalProgressionConfigurationReference(String key, Integer revision) {
    public ExternalProgressionConfigurationReference {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("configuration key is required");
        key = key.trim().toLowerCase(Locale.ROOT);
        if (key.length() > 255) throw new IllegalArgumentException("configuration key exceeds 255 characters");
        if (revision != null && revision < 1) throw new IllegalArgumentException("configuration revision must be positive");
    }
}
