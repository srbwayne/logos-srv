package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;

public interface ProgressionSubjectIdentityProvisioningPort {
    /**
     * Creates the native mapping for registration or the external mapping for the bounded POC self-link.
     * The persistence adapter records the corresponding initial classification and history.
     */
    void provision(ExternalSubjectReference reference, SubjectId target);
}
