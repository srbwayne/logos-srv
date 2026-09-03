package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;

import java.util.Optional;

public interface ProgressionConfigurationResolver {

    Optional<ProgressionConfiguration> resolve(ProgressionConfigurationReference reference);
}
