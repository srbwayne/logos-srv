package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.response;

import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;

import java.util.UUID;

public record ProgressionConfigurationResponse(
        UUID id,
        String logicalKey,
        Integer currentRevision,
        Long draftVersion,
        long activationVersion) {
    public static ProgressionConfigurationResponse from(ProgressionConfigurationDefinition value) {
        return new ProgressionConfigurationResponse(value.id(), value.logicalKey(), value.currentRevision(), value.draftVersion(), value.activationVersion());
    }
}
