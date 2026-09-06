package com.josecjuniors.logossrv.adapters.out.progression;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "progression_external_execution", uniqueConstraints =
        @UniqueConstraint(name = "uq_progression_external_execution_identity", columnNames = {"source_system", "idempotency_key"}))
public class ProgressionExternalExecutionEntity {
    @Id
    private UUID id;
    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;
    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;
    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;
    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;
    @Column(name = "processing_status", nullable = false, length = 32)
    private String processingStatus;
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "subject_namespace", nullable = false, length = 64)
    private String subjectNamespace;
    @Column(name = "subject_external_id", nullable = false, length = 255)
    private String subjectExternalId;
    @Column(name = "configuration_key", nullable = false, length = 255)
    private String configurationKey;
    @Column(name = "requested_revision")
    private Integer requestedRevision;
    @Column(name = "configuration_version_id", nullable = false)
    private UUID configurationVersionId;
    @Column(name = "skill_policy_version_id", nullable = false)
    private UUID skillPolicyVersionId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ProgressionExternalExecutionEntity() {}

    public ProgressionExternalExecutionEntity(UUID id, String sourceSystem, String idempotencyKey,
                                               String requestFingerprint, String responseJson,
                                               String subjectNamespace, String subjectExternalId,
                                               String configurationKey, Integer requestedRevision,
                                               UUID configurationVersionId, UUID skillPolicyVersionId) {
        this(id, sourceSystem, idempotencyKey, requestFingerprint, responseJson, "{}", "COMPLETED", 1, null,
                subjectNamespace, subjectExternalId, configurationKey, requestedRevision,
                configurationVersionId, skillPolicyVersionId);
    }

    public ProgressionExternalExecutionEntity(UUID id, String sourceSystem, String idempotencyKey,
                                               String requestFingerprint, String responseJson, String requestJson,
                                               String processingStatus, int attemptCount, String lastError,
                                               String subjectNamespace, String subjectExternalId,
                                               String configurationKey, Integer requestedRevision,
                                               UUID configurationVersionId, UUID skillPolicyVersionId) {
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.responseJson = responseJson;
        this.requestJson = requestJson;
        this.processingStatus = processingStatus;
        this.attemptCount = attemptCount;
        this.lastError = lastError;
        this.subjectNamespace = subjectNamespace;
        this.subjectExternalId = subjectExternalId;
        this.configurationKey = configurationKey;
        this.requestedRevision = requestedRevision;
        this.configurationVersionId = configurationVersionId;
        this.skillPolicyVersionId = skillPolicyVersionId;
        this.createdAt = LocalDateTime.now();
    }

    public String getRequestFingerprint() { return requestFingerprint; }
    public String getSourceSystem() { return sourceSystem; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSubjectNamespace() { return subjectNamespace; }
    public String getSubjectExternalId() { return subjectExternalId; }
    public String getConfigurationKey() { return configurationKey; }
    public Integer getRequestedRevision() { return requestedRevision; }
    public UUID getConfigurationVersionId() { return configurationVersionId; }
    public UUID getSkillPolicyVersionId() { return skillPolicyVersionId; }
    public String getResponseJson() { return responseJson; }
    public UUID getId() { return id; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }
    public String getRequestJson() { return requestJson; }
    public String getProcessingStatus() { return processingStatus; }
    public Integer getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public void markAttempt(String status, String error) {
        this.processingStatus = status;
        this.lastError = error;
        this.attemptCount++;
    }
    public void markCompleted(String responseJson) {
        this.responseJson = responseJson;
        this.processingStatus = "COMPLETED";
        this.lastError = null;
    }
}
