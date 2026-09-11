package com.josecjuniors.logossrv.adapters.in.web.progressionconfiguration.dto.response;

import com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository.ProgressionConfigurationAuthoringRepository.PublishedProgressionConfigurationVersion;

import java.util.UUID;

public record PublishedProgressionConfigurationResponse(UUID definitionId, UUID versionId, int revision,
                                                         long sourceDraftVersion) {
    public static PublishedProgressionConfigurationResponse from(PublishedProgressionConfigurationVersion version) {
        return new PublishedProgressionConfigurationResponse(version.definitionId(), version.versionId(), version.revision(), version.sourceDraftVersion());
    }
}
