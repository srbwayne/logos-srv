package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;

import java.util.Optional;

public interface ExternalProgressionConfigurationResolver {
    Optional<ResolvedProgressionConfiguration> resolve(ExternalProgressionConfigurationReference reference);
}
