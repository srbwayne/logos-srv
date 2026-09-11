package com.josecjuniors.logossrv.core.progressionconfiguration.domain.model;

import java.util.UUID;

public record ProgressionConfigurationDefinition(
        UUID id,
        String logicalKey,
        Integer currentRevision,
        Long draftVersion,
        long activationVersion,
        boolean legacyLinked) {
}
