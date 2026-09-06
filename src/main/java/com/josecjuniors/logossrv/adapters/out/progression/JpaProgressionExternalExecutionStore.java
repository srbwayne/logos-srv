package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExternalExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgressionExternalExecutionStore implements ProgressionExternalExecutionStore, ActivityProgressionExecutionStore {
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
            entity.markCompleted(json);
            repository.saveAndFlush(entity);
            return new StoredExecution(entity.getRequestFingerprint(), outcome);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not persist progression execution outcome", error);
        }
    }

    @Override
    public void create(ProgressionExecutionIdentity identity, String fingerprint, UUID subjectId,
                       com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact fact,
                       com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration resolved,
                       String configurationKey, Integer requestedRevision) {
        try {
            var existing = repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey());
            if (existing.isPresent()) {
                if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                    throw new ProgressionExecutionConflictException();
                }
                return;
            }
            String request = objectMapper.writeValueAsString(new ActivityRequest(subjectId, fact, resolved));
            repository.saveAndFlush(new ProgressionExternalExecutionEntity(UUID.randomUUID(), identity.source(),
                    identity.idempotencyKey(), fingerprint, "PENDING", request, "PENDING", 0, null,
                    "logos", subjectId.toString(), configurationKey, requestedRevision,
                    resolved.configurationVersionId(), resolved.skillPolicyVersionId()));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not persist activity progression intent", error);
        }
    }

    @Override
    public Optional<Execution> findActivity(ProgressionExecutionIdentity identity) {
        return repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .map(this::toActivityExecution);
    }

    @Override
    public Optional<Execution> findActivityForUpdate(ProgressionExecutionIdentity identity) {
        return repository.findBySourceSystemAndIdempotencyKeyForUpdate(identity.source(), identity.idempotencyKey())
                .map(this::toActivityExecution);
    }

    @Override
    public java.util.List<Execution> findUnresolved() {
        return repository.findByProcessingStatusInOrderByCreatedAtAsc(java.util.List.of("PENDING", "FAILED"))
                .stream().filter(e -> e.getSourceSystem().equals("logos.activity"))
                .map(this::toActivityExecution).toList();
    }

    @Override
    public void markAttempt(ProgressionExecutionIdentity identity) {
        repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .ifPresent(entity -> { entity.markAttempt("PROCESSING", null); repository.saveAndFlush(entity); });
    }

    @Override
    public void fail(ProgressionExecutionIdentity identity, String error) {
        repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .ifPresent(entity -> { entity.markAttempt("FAILED", error); repository.saveAndFlush(entity); });
    }

    private Execution toActivityExecution(ProgressionExternalExecutionEntity entity) {
        try {
            ActivityRequest request = objectMapper.readValue(entity.getRequestJson(), ActivityRequest.class);
            return new Execution(new ProgressionExecutionIdentity(entity.getSourceSystem(), entity.getIdempotencyKey()),
                    entity.getRequestFingerprint(), request.subjectId(), request.fact(), request.resolved(),
                    entity.getConfigurationKey(), entity.getRequestedRevision(), entity.getProcessingStatus(),
                    entity.getAttemptCount(), entity.getLastError());
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not recover activity progression intent", error);
        }
    }

    private record ActivityRequest(UUID subjectId,
                                   com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact fact,
                                   com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration resolved) {}

    private ProgressionOutcome read(String json) {
        try {
            return objectMapper.readValue(json, ProgressionOutcome.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not recover progression execution outcome", error);
        }
    }
}
