package com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository;

import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;

import java.util.Optional;
import java.util.UUID;

public interface ProgressionConfigurationAuthoringRepository {
    ProgressionConfigurationDefinition create(String logicalKey);
    Optional<ProgressionConfigurationDefinition> find(String logicalKey);
    Optional<ProgressionConfigurationDraft> findDraft(String logicalKey);
    ProgressionConfigurationDraft replaceDraft(String logicalKey, long expectedVersion, ProgressionConfigurationDraft draft);

    Optional<PublishedProgressionConfigurationVersion> findPublishedByDraftVersion(String logicalKey,
                                                                                    long sourceDraftVersion);

    Optional<ProgressionConfigurationDraft> lockDraft(String logicalKey, long expectedDraftVersion);

    PublishedProgressionConfigurationVersion publishLocked(String logicalKey, long sourceDraftVersion,
                                                           ProgressionConfigurationDraft draft);

    record PublishedProgressionConfigurationVersion(UUID definitionId, UUID versionId, int revision,
                                                    long sourceDraftVersion) {}
}
