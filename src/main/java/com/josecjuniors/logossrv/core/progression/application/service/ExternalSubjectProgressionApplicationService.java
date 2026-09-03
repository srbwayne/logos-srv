package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.stereotype.Service;

/** Resolves external identity before delegating to the existing progression flow. */
@Service
public class ExternalSubjectProgressionApplicationService implements ExecuteExternalSubjectProgressionUseCase {

    private final ExternalSubjectResolver resolver;
    private final ExecuteConfiguredSubjectProgressionUseCase progression;

    public ExternalSubjectProgressionApplicationService(ExternalSubjectResolver resolver,
                                                        ExecuteConfiguredSubjectProgressionUseCase progression) {
        this.resolver = resolver;
        this.progression = progression;
    }

    @Override
    public ProgressionOutcome execute(ExternalSubjectReference subjectReference,
                                      ProgressionConfigurationReference configurationReference,
                                      ProgressionFact fact) {
        return progression.execute(resolver.resolve(subjectReference), configurationReference, fact);
    }
}
