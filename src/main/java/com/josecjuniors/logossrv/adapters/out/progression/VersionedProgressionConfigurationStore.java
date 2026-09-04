package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;

import java.util.Optional;
import java.util.UUID;

interface VersionedProgressionConfigurationStore {
    Optional<ResolvedProgressionConfiguration> resolveVersioned(ProgressionConfigurationReference reference);
    void snapshotConfiguration(UUID legacyId);
    void snapshotSkillPolicy();
}
