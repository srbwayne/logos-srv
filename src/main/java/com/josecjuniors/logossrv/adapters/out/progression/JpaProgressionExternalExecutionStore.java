package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExternalExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgressionExternalExecutionStore implements ProgressionExternalExecutionStore {
    private final ProgressionExternalExecutionJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaProgressionExternalExecutionStore(ProgressionExternalExecutionJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<StoredExecution> find(ProgressionExecutionIdentity identity) {
        return repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .map(entity -> new StoredExecution(entity.getRequestFingerprint(), read(entity.getResponseJson())));
    }

    @Override
    public UUID reserve(ProgressionExecutionIdentity identity, String fingerprint, String subjectNamespace,
                        String subjectExternalId, String configurationKey, Integer requestedRevision,
                        UUID configurationVersionId, UUID skillPolicyVersionId) {
        UUID id = UUID.randomUUID();
        try {
            repository.saveAndFlush(new ProgressionExternalExecutionEntity(id, identity.source(), identity.idempotencyKey(),
                    fingerprint, "PENDING", subjectNamespace, subjectExternalId, configurationKey, requestedRevision,
                    configurationVersionId, skillPolicyVersionId));
            return id;
        } catch (DataIntegrityViolationException error) {
            throw error;
        }
    }

    @Override
    public StoredExecution complete(ProgressionExecutionIdentity identity, ProgressionOutcome outcome) {
        try {
            String json = objectMapper.writeValueAsString(outcome);
            var entity = repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Reserved progression execution not found"));
            entity.setResponseJson(json);
            repository.saveAndFlush(entity);
            return new StoredExecution(entity.getRequestFingerprint(), outcome);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not persist progression execution outcome", error);
        }
    }

    private ProgressionOutcome read(String json) {
        try {
            return objectMapper.readValue(json, ProgressionOutcome.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not recover progression execution outcome", error);
        }
    }
}
