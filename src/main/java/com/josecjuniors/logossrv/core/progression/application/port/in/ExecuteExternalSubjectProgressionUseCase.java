package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;

public interface ExecuteExternalSubjectProgressionUseCase {

    ProgressionOutcome execute(ExternalSubjectReference subjectReference,
                               ProgressionConfigurationReference configurationReference,
                               ProgressionFact fact);
}
