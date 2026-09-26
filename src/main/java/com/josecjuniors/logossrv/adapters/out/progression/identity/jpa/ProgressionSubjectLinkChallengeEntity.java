package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progression_subject_link_challenge")
public class ProgressionSubjectLinkChallengeEntity {
    @Id private UUID id;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(nullable = false, length = 64) private String namespace;
    @Column(name = "jogador_id", nullable = false) private UUID jogadorId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;

    protected ProgressionSubjectLinkChallengeEntity() { }
    public ProgressionSubjectLinkChallengeEntity(UUID id, String tokenHash, String namespace, UUID jogadorId,
                                                  Instant createdAt, Instant expiresAt) {
        this.id = id; this.tokenHash = tokenHash; this.namespace = namespace; this.jogadorId = jogadorId;
        this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    public UUID getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public String getNamespace() { return namespace; }
    public UUID getJogadorId() { return jogadorId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void consume(Instant at) { this.consumedAt = at; }
}
