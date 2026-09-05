package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;

public interface ExecuteIdempotentExternalSubjectProgressionUseCase {
    ProgressionOutcome execute(ProgressionExecutionIdentity identity, ExternalSubjectReference subject,
                               ExternalProgressionConfigurationReference configuration, ProgressionFact fact);
}
