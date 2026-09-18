package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;

public interface ProgressionSubjectIdentityProvisioningPort {
    void provision(ExternalSubjectReference reference, SubjectId target);
}
