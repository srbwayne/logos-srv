package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;

import java.util.Optional;

public interface VersionedProgressionConfigurationResolver {
    Optional<ResolvedProgressionConfiguration> resolveVersioned(ProgressionConfigurationReference reference);

    default Optional<ResolvedProgressionConfiguration> resolveLegacyVersioned(ProgressionConfigurationReference reference) {
        return resolveVersioned(reference);
    }
}
