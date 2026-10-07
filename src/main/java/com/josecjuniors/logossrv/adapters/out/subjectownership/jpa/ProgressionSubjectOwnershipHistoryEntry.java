package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipProvenance;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipClassification;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipVerificationEvidence;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAdministrativeReason;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipTransferEvidence;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "progression_subject_ownership_history", uniqueConstraints =
        @UniqueConstraint(name = "uq_progression_subject_ownership_history_version",
                columnNames = {"identity_id", "aggregate_version"}))
public class ProgressionSubjectOwnershipHistoryEntry {

    @Id
    private UUID id;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_identity_class", length = 32)
    private IdentityClass previousIdentityClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_identity_class", nullable = false, length = 32)
    private IdentityClass newIdentityClass;

    @Column(name = "previous_target_jogador_id")
    private UUID previousTargetJogadorId;

    @Column(name = "new_target_jogador_id", nullable = false)
    private UUID newTargetJogadorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_ownership_status", length = 32)
    private OwnershipStatus previousOwnershipStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_ownership_status", nullable = false, length = 32)
    private OwnershipStatus newOwnershipStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_verification_status", length = 32)
    private VerificationStatus previousVerificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_verification_status", nullable = false, length = 32)
    private VerificationStatus newVerificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private OwnershipProvenance provenance;

    @Column(name = "actor_type", length = 32)
    private String actorType;

    @Column(name = "actor_id", length = 128)
    private String actorId;

    @Column(name = "evidence_type", length = 64)
    private String evidenceType;

    @Column(name = "evidence_reference", length = 255)
    private String evidenceReference;

    @Column(length = 512)
    private String reason;

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "recorded_at", nullable = false, insertable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "predecessor_identity_id")
    private UUID predecessorIdentityId;

    @Column(name = "predecessor_ownership_version")
    private Long predecessorOwnershipVersion;

    @Column(name = "reassignment_request_id")
    private UUID reassignmentRequestId;

    @Column(name = "reassignment_authorization_id")
    private UUID reassignmentAuthorizationId;

    protected ProgressionSubjectOwnershipHistoryEntry() {
    }

    private ProgressionSubjectOwnershipHistoryEntry(UUID id, UUID identityId, UUID targetJogadorId,
                                                    SubjectOwnershipClassification classification,
                                                    UUID actionActorAppUserId, Instant effectiveAt) {
        this.id = id;
        this.identityId = identityId;
        this.aggregateVersion = 0L;
        this.eventType = "INITIAL_CLASSIFICATION";
        this.newIdentityClass = classification.identityClass();
        this.newTargetJogadorId = targetJogadorId;
        this.newOwnershipStatus = classification.ownershipStatus();
        this.newVerificationStatus = classification.verificationStatus();
        this.provenance = classification.provenance();
        this.actorType = actionActorAppUserId == null ? null : "APP_USER";
        this.actorId = actionActorAppUserId == null ? null : actionActorAppUserId.toString();
        this.effectiveAt = effectiveAt;
    }

    public static ProgressionSubjectOwnershipHistoryEntry initial(UUID identityId, UUID targetJogadorId,
                                                                  SubjectOwnershipClassification classification,
                                                                  UUID actionActorAppUserId,
                                                                  Instant effectiveAt) {
        return new ProgressionSubjectOwnershipHistoryEntry(UUID.randomUUID(), identityId, targetJogadorId,
                classification, actionActorAppUserId, effectiveAt);
    }

    public static ProgressionSubjectOwnershipHistoryEntry verified(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_VERIFIED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = evidence.evidenceType();
        entry.evidenceReference = evidence.evidenceReference();
        entry.reason = evidence.reason();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry invalidated(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_VERIFICATION_INVALIDATED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = evidence.evidenceType();
        entry.evidenceReference = evidence.evidenceReference();
        entry.reason = evidence.reason();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry reverified(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            OwnershipVerificationEvidence evidence, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_REVERIFIED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = evidence.evidenceType();
        entry.evidenceReference = evidence.evidenceReference();
        entry.reason = evidence.reason();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry disabled(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_DISABLED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = null;
        entry.evidenceReference = null;
        entry.reason = reason.value();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry reactivated(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_REACTIVATED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = null;
        entry.evidenceReference = null;
        entry.reason = reason.value();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry revoked(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            SubjectOwnershipAdministrativeReason reason, Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_REVOKED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = null;
        entry.evidenceReference = null;
        entry.reason = reason.value();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public static ProgressionSubjectOwnershipHistoryEntry transferred(SubjectOwnershipAggregate before,
            SubjectOwnershipAggregate after, AuthorizedSubjectOwnershipOperator operator,
            SubjectOwnershipTransferEvidence evidence, SubjectOwnershipAdministrativeReason reason,
            Instant effectiveAt) {
        var entry = new ProgressionSubjectOwnershipHistoryEntry();
        entry.id = UUID.randomUUID();
        entry.identityId = before.id();
        entry.aggregateVersion = after.ownershipVersion();
        entry.eventType = "OWNERSHIP_TRANSFERRED";
        entry.previousIdentityClass = before.identityClass();
        entry.newIdentityClass = after.identityClass();
        entry.previousTargetJogadorId = before.targetJogadorId();
        entry.newTargetJogadorId = after.targetJogadorId();
        entry.previousOwnershipStatus = before.ownershipStatus();
        entry.newOwnershipStatus = after.ownershipStatus();
        entry.previousVerificationStatus = before.verificationStatus();
        entry.newVerificationStatus = after.verificationStatus();
        entry.provenance = OwnershipProvenance.LOGOS_OPERATOR_ACTION;
        entry.actorType = operator.auditActorType();
        entry.actorId = operator.principalId();
        entry.evidenceType = SubjectOwnershipTransferEvidence.TYPE;
        entry.evidenceReference = evidence.reference();
        entry.reason = reason.value();
        entry.effectiveAt = effectiveAt;
        return entry;
    }

    public boolean matchesTransferReplay(SubjectOwnershipAggregate current, long expectedOwnershipVersion,
            UUID requestedTarget, SubjectOwnershipTransferEvidence evidence,
            SubjectOwnershipAdministrativeReason requestedReason) {
        if (expectedOwnershipVersion == Long.MAX_VALUE
                || current.ownershipVersion() != expectedOwnershipVersion + 1
                || aggregateVersion != current.ownershipVersion()
                || !Objects.equals(identityId, current.id())
                || !"OWNERSHIP_TRANSFERRED".equals(eventType)
                || provenance != OwnershipProvenance.LOGOS_OPERATOR_ACTION
                || !"WORKLOAD_OPERATOR".equals(actorType) || actorId == null || actorId.isBlank()) return false;

        if (previousIdentityClass != IdentityClass.EXTERNAL || newIdentityClass != IdentityClass.EXTERNAL
                || current.identityClass() != IdentityClass.EXTERNAL
                || !Objects.equals(newTargetJogadorId, requestedTarget)
                || !Objects.equals(current.targetJogadorId(), requestedTarget)
                || previousTargetJogadorId == null || previousTargetJogadorId.equals(requestedTarget)
                || previousOwnershipStatus == null || previousOwnershipStatus != newOwnershipStatus
                || newOwnershipStatus != current.ownershipStatus()
                || !isSupportedTransferVerification(previousVerificationStatus)
                || newVerificationStatus != transferredVerification(previousVerificationStatus)
                || newVerificationStatus != current.verificationStatus()) return false;

        return SubjectOwnershipTransferEvidence.TYPE.equals(evidenceType)
                && Objects.equals(evidenceReference, evidence.reference())
                && Objects.equals(reason, requestedReason.value());
    }

    private static boolean isSupportedTransferVerification(VerificationStatus status) {
        return status == VerificationStatus.UNVERIFIED || status == VerificationStatus.VERIFIED
                || status == VerificationStatus.INVALIDATED;
    }

    private static VerificationStatus transferredVerification(VerificationStatus status) {
        return status == VerificationStatus.VERIFIED ? VerificationStatus.UNVERIFIED : status;
    }
}
