package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by_client_id", length = 64)
    private String verifiedByClientId;

    protected ProgressionSubjectIdentity() {
    }

    public ProgressionSubjectIdentity(UUID id, String namespace, String externalId, Jogador jogador) {
        this.id = id;
        this.namespace = namespace;
        this.externalId = externalId;
        this.jogador = jogador;
        this.verificationStatus = "logos-native".equals(namespace) ? "LOGOS_NATIVE" : "UNVERIFIED";
    }

    public static ProgressionSubjectIdentity integrationVerified(UUID id, String namespace, String externalId,
                                                                  Jogador jogador, Instant verifiedAt, String clientId) {
        var identity = new ProgressionSubjectIdentity(id, namespace, externalId, jogador);
        identity.verificationStatus = "INTEGRATION_VERIFIED";
        identity.verifiedAt = verifiedAt;
        identity.verifiedByClientId = clientId;
        return identity;
    }

    public String getVerificationStatus() { return verificationStatus; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getVerifiedByClientId() { return verifiedByClientId; }
    public UUID getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getExternalId() { return externalId; }

    public Jogador getJogador() {
        return jogador;
    }
}
