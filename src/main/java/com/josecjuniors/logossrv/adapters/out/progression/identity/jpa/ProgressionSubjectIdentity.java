package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipClassification;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "progression_subject_identity")
public class ProgressionSubjectIdentity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String namespace;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_class", nullable = false, length = 32)
    private IdentityClass identityClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_status", nullable = false, length = 32)
    private OwnershipStatus ownershipStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus;

    @Column(name = "ownership_version", nullable = false)
    private long ownershipVersion;

    protected ProgressionSubjectIdentity() {
    }

    public ProgressionSubjectIdentity(UUID id, String namespace, String externalId, Jogador jogador) {
        this.id = id;
        this.namespace = namespace;
        this.externalId = externalId;
        this.jogador = jogador;
        var classification = SubjectOwnershipClassification.legacy(namespace);
        this.identityClass = classification.identityClass();
        this.ownershipStatus = classification.ownershipStatus();
        this.verificationStatus = classification.verificationStatus();
        this.ownershipVersion = 0L;
    }

    public Jogador getJogador() {
        return jogador;
    }

    public UUID getId() { return id; }

    public String getNamespace() { return namespace; }

    public String getExternalId() { return externalId; }

    public IdentityClass getIdentityClass() {
        return identityClass;
    }

    public OwnershipStatus getOwnershipStatus() {
        return ownershipStatus;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public long getOwnershipVersion() {
        return ownershipVersion;
    }

    public void applyVerification(VerificationStatus nextVerificationStatus, long nextOwnershipVersion) {
        this.verificationStatus = nextVerificationStatus;
        this.ownershipVersion = nextOwnershipVersion;
    }

    public void applyOwnershipStatus(OwnershipStatus nextOwnershipStatus, long nextOwnershipVersion) {
        this.ownershipStatus = nextOwnershipStatus;
        this.ownershipVersion = nextOwnershipVersion;
    }
}
