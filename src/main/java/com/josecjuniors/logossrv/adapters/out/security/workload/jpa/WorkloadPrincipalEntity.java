package com.josecjuniors.logossrv.adapters.out.security.workload.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workload_principal")
public class WorkloadPrincipalEntity {
    @Id
    private UUID id;

    @Column(name = "principal_type", nullable = false, length = 32)
    private String principalType;

    @Column(name = "principal_id", nullable = false, length = 128)
    private String principalId;

    @Column(nullable = false, length = 255)
    private String issuer;

    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private String lifecycleStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected WorkloadPrincipalEntity() {
    }

    public UUID getId() { return id; }
    public String getPrincipalType() { return principalType; }
    public String getPrincipalId() { return principalId; }
    public String getIssuer() { return issuer; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDisabledAt() { return disabledAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
