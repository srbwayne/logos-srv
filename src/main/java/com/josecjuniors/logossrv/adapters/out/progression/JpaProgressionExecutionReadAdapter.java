package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionReadCorruptedException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaProgressionExecutionReadAdapter implements ProgressionExecutionReadPort {
    private final ProgressionExternalExecutionJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaProgressionExecutionReadAdapter(ProgressionExternalExecutionJpaRepository repository,
                                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ProgressionExecutionRead> find(ProgressionExecutionIdentity identity) {
        return repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .map(this::map);
    }

    private ProgressionExecutionRead map(ProgressionExternalExecutionEntity entity) {
        var status = status(entity.getProcessingStatus());
        var outcome = status == ProgressionExecutionStatus.COMPLETED
                ? readOutcome(entity.getResponseJson())
                : null;
        return new ProgressionExecutionRead(
                new ProgressionExecutionIdentity(entity.getSourceSystem(), entity.getIdempotencyKey()),
                new ExternalSubjectReference(entity.getSubjectNamespace(), entity.getSubjectExternalId()),
                entity.getConfigurationKey(),
                entity.getRequestedRevision(),
                entity.getConfigurationVersionId(),
                entity.getSkillPolicyVersionId(),
                status,
                outcome);
    }

    private ProgressionExecutionStatus status(String value) {
        try {
            return ProgressionExecutionStatus.fromPersistence(value);
        } catch (RuntimeException exception) {
            throw new ProgressionExecutionReadCorruptedException(exception);
        }
    }

    private ProgressionOutcome readOutcome(String json) {
        try {
            return objectMapper.readValue(json, ProgressionOutcome.class);
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new ProgressionExecutionReadCorruptedException(exception);
        }
    }
}
