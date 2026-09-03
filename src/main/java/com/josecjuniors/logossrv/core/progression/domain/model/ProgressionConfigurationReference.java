package com.josecjuniors.logossrv.core.progression.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Referência opaca à configuração persistida que define uma progressão. */
public record ProgressionConfigurationReference(UUID value) {

    public ProgressionConfigurationReference {
        Objects.requireNonNull(value, "configurationReference");
    }
}
