package com.josecjuniors.logossrv.core.progression.application.port.out;

import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityProgressionExecutionStore {
    void create(ProgressionExecutionIdentity identity, String fingerprint, UUID subjectId,
                ProgressionFact fact, ResolvedProgressionConfiguration resolved,
                String configurationKey, Integer requestedRevision);

    Optional<Execution> findActivity(ProgressionExecutionIdentity identity);

    Optional<Execution> findActivityForUpdate(ProgressionExecutionIdentity identity);

    List<Execution> findUnresolved();

    void markAttempt(ProgressionExecutionIdentity identity);

    ProgressionExternalExecutionStore.StoredExecution complete(ProgressionExecutionIdentity identity,
                                                                 com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome outcome);

    void fail(ProgressionExecutionIdentity identity, String error);

    record Execution(ProgressionExecutionIdentity identity, String fingerprint, UUID subjectId,
                     ProgressionFact fact, ResolvedProgressionConfiguration resolved,
                     String configurationKey, Integer requestedRevision, String status,
                     int attemptCount, String lastError) {}
}
