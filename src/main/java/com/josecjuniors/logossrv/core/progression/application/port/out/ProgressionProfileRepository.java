package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;

import java.util.Optional;

/** Porta de persistência do estado de progressão, sem expor o modelo atual. */
public interface ProgressionProfileRepository {

    Optional<ProgressionProfile> findBySubjectId(SubjectId subjectId);

    Optional<ProgressionProfile> findBySubjectIdForUpdate(SubjectId subjectId);

    ProgressionProfile save(SubjectId subjectId, ProgressionProfile profile);
}
