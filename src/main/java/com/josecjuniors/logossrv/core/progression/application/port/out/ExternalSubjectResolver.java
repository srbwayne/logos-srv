package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;

public interface ExternalSubjectResolver {

    SubjectId resolve(ExternalSubjectReference reference);
}
