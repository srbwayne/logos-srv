package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;

import java.util.Optional;
import java.util.UUID;

interface VersionedProgressionConfigurationStore {
    Optional<ResolvedProgressionConfiguration> resolveVersioned(ProgressionConfigurationReference reference);
    Optional<ResolvedProgressionConfiguration> resolveLegacyVersioned(ProgressionConfigurationReference reference);
    Optional<ResolvedProgressionConfiguration> resolveExternal(ExternalProgressionConfigurationReference reference);
    void snapshotConfiguration(UUID legacyId);
    void snapshotSkillPolicy();
}
