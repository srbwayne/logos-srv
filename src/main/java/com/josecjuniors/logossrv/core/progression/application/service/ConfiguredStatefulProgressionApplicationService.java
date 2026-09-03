package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;

/** Orquestra facts e configuraÃ§Ã£o resolvida antes de delegar a progressÃ£o stateful. */
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
                .orElseThrow(() -> new IllegalStateException("Estado de progressÃ£o nÃ£o encontrado para o subject."));
        var configuration = configurationResolver.resolve(configurationReference)
                .orElseThrow(() -> new IllegalStateException("ConfiguraÃ§Ã£o de progressÃ£o nÃ£o encontrada."));
        return statefulProgression.executeLoaded(subjectId, inputFactory.create(fact, configuration, profile), profile);
    }
}
