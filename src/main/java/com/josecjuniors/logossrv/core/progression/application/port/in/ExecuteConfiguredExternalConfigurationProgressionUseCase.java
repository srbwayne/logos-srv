package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;

public interface ExecuteConfiguredExternalConfigurationProgressionUseCase {
    ProgressionOutcome execute(SubjectId subjectId, ExternalProgressionConfigurationReference configuration, ProgressionFact fact);
}
