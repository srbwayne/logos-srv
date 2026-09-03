package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import org.springframework.stereotype.Service;

/** Orquestra facts e configuraÃ§Ã£o resolvida antes de delegar a progressÃ£o stateful. */
@Service
public class ConfiguredStatefulProgressionApplicationService implements ExecuteConfiguredSubjectProgressionUseCase {

    private final ProgressionProfileRepository profileRepository;
    private final ProgressionConfigurationResolver configurationResolver;
    private final ProgressionInputFactory inputFactory;
    private final StatefulProgressionApplicationService statefulProgression;

    public ConfiguredStatefulProgressionApplicationService(
            ProgressionProfileRepository profileRepository,
            ProgressionConfigurationResolver configurationResolver,
            ProgressionInputFactory inputFactory,
            StatefulProgressionApplicationService statefulProgression) {
        this.profileRepository = profileRepository;
        this.configurationResolver = configurationResolver;
        this.inputFactory = inputFactory;
        this.statefulProgression = statefulProgression;
    }

    @Override
    public ProgressionOutcome execute(SubjectId subjectId, ProgressionConfigurationReference configurationReference,
                                      ProgressionFact fact) {
        ProgressionProfile profile = profileRepository.findBySubjectId(subjectId)
                .orElseThrow(ProgressionSubjectNotFoundException::new);
        var configuration = configurationResolver.resolve(configurationReference)
                .orElseThrow(ProgressionConfigurationNotFoundException::new);
        return statefulProgression.executeLoaded(subjectId, inputFactory.create(fact, configuration, profile), profile);
    }
}
