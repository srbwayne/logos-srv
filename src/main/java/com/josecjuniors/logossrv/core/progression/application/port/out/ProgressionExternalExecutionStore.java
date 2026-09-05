package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;

import java.util.Optional;
import java.util.UUID;

public interface ProgressionExternalExecutionStore {
    Optional<StoredExecution> find(ProgressionExecutionIdentity identity);

    UUID reserve(ProgressionExecutionIdentity identity, String fingerprint,
                 String subjectNamespace, String subjectExternalId,
                 String configurationKey, Integer requestedRevision,
                 UUID configurationVersionId, UUID skillPolicyVersionId);

    StoredExecution complete(ProgressionExecutionIdentity identity, ProgressionOutcome outcome);

    record StoredExecution(String fingerprint, ProgressionOutcome outcome) {}
}
