package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.springframework.stereotype.Service;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteProgressionUseCase;

/** Carrega, executa e persiste a progressão canônica de um subject. */
@Service
public class StatefulProgressionApplicationService implements ExecuteSubjectProgressionUseCase {

    private final ProgressionProfileRepository profileRepository;
    private final ExecuteProgressionUseCase progressionUseCase;

    public StatefulProgressionApplicationService(ProgressionProfileRepository profileRepository,
                                                  ExecuteProgressionUseCase progressionUseCase) {
        this.profileRepository = profileRepository;
        this.progressionUseCase = progressionUseCase;
    }

    @Override
    public ProgressionOutcome execute(SubjectId subjectId, ProgressionInput input) {
        ProgressionProfile currentProfile = profileRepository.findBySubjectIdForUpdate(subjectId)
                .orElseThrow(ProgressionSubjectNotFoundException::new);
        return executeLoaded(subjectId, input, currentProfile);
    }

    public ProgressionOutcome executeLoaded(SubjectId subjectId, ProgressionInput input,
                                            ProgressionProfile currentProfile) {
        ProgressionOutcome calculated = progressionUseCase.execute(input, currentProfile);
        ProgressionProfile savedProfile = profileRepository.save(subjectId, calculated.updatedProfile());
        return new ProgressionOutcome(calculated.result(), savedProfile);
    }
}
