package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence mapping for an immutable reviewed target-correction authorization. */
@Entity
@Table(name = "progression_subject_target_correction_authorization")
public class ProgressionSubjectTargetCorrectionAuthorization {

    @Id
    @Column(name = "authorization_id", nullable = false, updatable = false)
    private UUID authorizationId;

    @Column(name = "correction_request_id", nullable = false, updatable = false, unique = true)
    private UUID correctionRequestId;

    @Column(nullable = false, length = 64, updatable = false)
    private String namespace;

    @Column(name = "external_id", nullable = false, length = 255, updatable = false)
    private String externalId;

    @Column(name = "predecessor_identity_id", nullable = false, updatable = false)
    private UUID predecessorIdentityId;

    @Column(name = "predecessor_ownership_version", nullable = false, updatable = false)
    private long predecessorOwnershipVersion;

    @Column(name = "predecessor_target_jogador_id", nullable = false, updatable = false)
    private UUID predecessorTargetJogadorId;

    @Column(name = "corrected_target_jogador_id", nullable = false, updatable = false)
    private UUID correctedTargetJogadorId;

    @Column(name = "correction_basis", nullable = false, length = 2048, updatable = false)
    private String correctionBasis;

    @Column(name = "source_assertion_reference", nullable = false, length = 512, updatable = false)
    private String sourceAssertionReference;

    @Column(name = "authoritative_fact_reference", nullable = false, length = 512, updatable = false)
    private String authoritativeFactReference;

    @Column(name = "reviewed_case_reference", nullable = false, length = 255, updatable = false)
    private String reviewedCaseReference;

    @Column(name = "reviewer_principal_id", nullable = false, length = 128, updatable = false)
    private String reviewerPrincipalId;

    @Column(name = "reviewed_at", nullable = false, insertable = false, updatable = false)
    private Instant reviewedAt;

    protected ProgressionSubjectTargetCorrectionAuthorization() {
    }

    public ProgressionSubjectTargetCorrectionAuthorization(UUID authorizationId, UUID correctionRequestId,
            String namespace, String externalId, UUID predecessorIdentityId, long predecessorOwnershipVersion,
            UUID predecessorTargetJogadorId, UUID correctedTargetJogadorId, String correctionBasis,
            String sourceAssertionReference, String authoritativeFactReference, String reviewedCaseReference,
            String reviewerPrincipalId) {
        this.authorizationId = authorizationId;
        this.correctionRequestId = correctionRequestId;
        this.namespace = namespace;
        this.externalId = externalId;
        this.predecessorIdentityId = predecessorIdentityId;
        this.predecessorOwnershipVersion = predecessorOwnershipVersion;
        this.predecessorTargetJogadorId = predecessorTargetJogadorId;
        this.correctedTargetJogadorId = correctedTargetJogadorId;
        this.correctionBasis = correctionBasis;
        this.sourceAssertionReference = sourceAssertionReference;
        this.authoritativeFactReference = authoritativeFactReference;
        this.reviewedCaseReference = reviewedCaseReference;
        this.reviewerPrincipalId = reviewerPrincipalId;
    }

    public UUID getAuthorizationId() { return authorizationId; }
    public UUID getCorrectionRequestId() { return correctionRequestId; }
    public String getNamespace() { return namespace; }
    public String getExternalId() { return externalId; }
    public UUID getPredecessorIdentityId() { return predecessorIdentityId; }
    public long getPredecessorOwnershipVersion() { return predecessorOwnershipVersion; }
    public UUID getPredecessorTargetJogadorId() { return predecessorTargetJogadorId; }
    public UUID getCorrectedTargetJogadorId() { return correctedTargetJogadorId; }
    public String getCorrectionBasis() { return correctionBasis; }
    public String getSourceAssertionReference() { return sourceAssertionReference; }
    public String getAuthoritativeFactReference() { return authoritativeFactReference; }
    public String getReviewedCaseReference() { return reviewedCaseReference; }
    public String getReviewerPrincipalId() { return reviewerPrincipalId; }
    public Instant getReviewedAt() { return reviewedAt; }
}
