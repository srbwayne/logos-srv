package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteIdempotentExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExternalExecutionStore;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class IdempotentExternalSubjectProgressionApplicationService implements ExecuteIdempotentExternalSubjectProgressionUseCase {
    private final ConfiguredStatefulProgressionApplicationService configuredService;
    private final ProgressionExternalExecutionStore executions;
    private final IdempotentExternalSubjectProgressionTransaction transaction;

    public IdempotentExternalSubjectProgressionApplicationService(
            ConfiguredStatefulProgressionApplicationService configuredService,
            ProgressionExternalExecutionStore executions,
            IdempotentExternalSubjectProgressionTransaction transaction) {
        this.configuredService = configuredService;
        this.executions = executions;
        this.transaction = transaction;
    }

    @Override
    public ProgressionOutcome execute(ProgressionExecutionIdentity identity, ExternalSubjectReference subject,
                                      ExternalProgressionConfigurationReference configuration, ProgressionFact fact) {
        String fingerprint = ProgressionExecutionFingerprint.of(identity, subject, configuration, fact);
        var existing = executions.find(identity);
        if (existing.isPresent()) return existing.get().fingerprint().equals(fingerprint)
                ? existing.get().outcome() : conflict();
        try {
            return transaction.process(identity, fingerprint, subject, configuration, fact);
        } catch (DataIntegrityViolationException duplicate) {
            return executions.find(identity)
                    .filter(value -> value.fingerprint().equals(fingerprint))
                    .map(ProgressionExternalExecutionStore.StoredExecution::outcome)
                    .orElseThrow(() -> duplicate);
        }
    }

    private ProgressionOutcome conflict() { throw new ProgressionExecutionConflictException(); }

    @Service
    static class IdempotentExternalSubjectProgressionTransaction {
        private final ExternalProgressionConfigurationResolver configurationResolver;
        private final ConfiguredStatefulProgressionApplicationService configuredService;
        private final ProgressionExternalExecutionStore executions;
        private final com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver subjectResolver;

        IdempotentExternalSubjectProgressionTransaction(ExternalProgressionConfigurationResolver configurationResolver,
                                                        ConfiguredStatefulProgressionApplicationService configuredService,
                                                        ProgressionExternalExecutionStore executions,
                                                        com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver subjectResolver) {
            this.configurationResolver = configurationResolver;
            this.configuredService = configuredService;
            this.executions = executions;
            this.subjectResolver = subjectResolver;
        }

        @org.springframework.transaction.annotation.Transactional
        ProgressionOutcome process(ProgressionExecutionIdentity identity, String fingerprint,
                                    ExternalSubjectReference subject, ExternalProgressionConfigurationReference configuration,
                                    ProgressionFact fact) {
            var resolved = configurationResolver.resolve(configuration)
                    .orElseThrow(ProgressionConfigurationNotFoundException::new);
            var subjectId = subjectResolver.resolve(subject);
            executions.reserve(identity, fingerprint, subject.namespace(), subject.externalId(), configuration.key(),
                    configuration.revision(), resolved.configurationVersionId(), resolved.skillPolicyVersionId());
            var outcome = configuredService.executeResolved(subjectId, resolved, fact);
            executions.complete(identity, outcome);
            return outcome;
        }
    }
}
