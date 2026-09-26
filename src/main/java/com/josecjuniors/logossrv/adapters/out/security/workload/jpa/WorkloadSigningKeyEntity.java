package com.josecjuniors.logossrv.adapters.out.security.workload.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workload_signing_key")
public class WorkloadSigningKeyEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_principal_id", nullable = false)
    private WorkloadPrincipalEntity principal;

    @Column(nullable = false, length = 64)
    private String kid;

    @Column(nullable = false, length = 16)
    private String algorithm;

    @Column(name = "public_key_pem", nullable = false, columnDefinition = "text")
    private String publicKeyPem;

    @Column(name = "public_key_spki_sha256", nullable = false)
    private byte[] publicKeySpkiSha256;

    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private String lifecycleStatus;

    @Column(name = "not_before", nullable = false)
    private Instant notBefore;

    @Column(name = "not_after")
    private Instant notAfter;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkloadSigningKeyEntity() {
    }

    public UUID getId() { return id; }
    public WorkloadPrincipalEntity getPrincipal() { return principal; }
    public String getKid() { return kid; }
    public String getAlgorithm() { return algorithm; }
    public String getPublicKeyPem() { return publicKeyPem; }
    public byte[] getPublicKeySpkiSha256() { return publicKeySpkiSha256; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public Instant getNotBefore() { return notBefore; }
    public Instant getNotAfter() { return notAfter; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getRetiredAt() { return retiredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
