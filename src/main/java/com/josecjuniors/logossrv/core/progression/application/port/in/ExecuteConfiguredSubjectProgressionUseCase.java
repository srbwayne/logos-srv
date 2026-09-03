package com.josecjuniors.logossrv.core.progression.application.port.in;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;

public interface ExecuteConfiguredSubjectProgressionUseCase {

    ProgressionOutcome execute(SubjectId subjectId, ProgressionConfigurationReference configurationReference,
                               ProgressionFact fact);
}
