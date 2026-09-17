package com.josecjuniors.logossrv.core.progression.application.query;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;

import java.util.UUID;

public record ProgressionExecutionRead(
        ProgressionExecutionIdentity identity,
        ExternalSubjectReference subject,
        String configurationKey,
        Integer requestedRevision,
        UUID configurationVersionId,
        UUID skillPolicyVersionId,
        ProgressionExecutionStatus status,
        ProgressionOutcome outcome) {
}
