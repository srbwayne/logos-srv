package com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository;

import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDraft;

import java.util.Optional;

public interface ProgressionConfigurationAuthoringRepository {
    ProgressionConfigurationDefinition create(String logicalKey);
    Optional<ProgressionConfigurationDefinition> find(String logicalKey);
    Optional<ProgressionConfigurationDraft> findDraft(String logicalKey);
    ProgressionConfigurationDraft replaceDraft(String logicalKey, long expectedVersion, ProgressionConfigurationDraft draft);
}
