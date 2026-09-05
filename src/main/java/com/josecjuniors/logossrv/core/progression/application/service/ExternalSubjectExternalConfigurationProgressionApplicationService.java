package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteExternalSubjectExternalConfigurationProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredExternalConfigurationProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.stereotype.Service;

@Service
public class ExternalSubjectExternalConfigurationProgressionApplicationService implements ExecuteExternalSubjectExternalConfigurationProgressionUseCase {
    private final ExternalSubjectResolver subjectResolver;
    private final ExecuteConfiguredExternalConfigurationProgressionUseCase configuredProgression;

    public ExternalSubjectExternalConfigurationProgressionApplicationService(ExternalSubjectResolver subjectResolver,
                                                                               ExecuteConfiguredExternalConfigurationProgressionUseCase configuredProgression) {
        this.subjectResolver = subjectResolver;
        this.configuredProgression = configuredProgression;
    }

    @Override
    public ProgressionOutcome execute(ExternalSubjectReference subject, ExternalProgressionConfigurationReference configuration,
                                      ProgressionFact fact) {
        return configuredProgression.execute(subjectResolver.resolve(subject), configuration, fact);
    }
}
