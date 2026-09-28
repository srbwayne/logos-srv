package com.josecjuniors.logossrv.core.security.workload.admin.application.port.out;

import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface WorkloadTrustAdministrationStore {
    RegistrationResult registerPrincipal(WorkloadPrincipalId principalId, WorkloadIssuer issuer,
                                         TrustAdministrationActor actor, TrustAdministrationReason reason,
                                         Instant now);

    RegistrationResult registerCredential(WorkloadPrincipalId principalId, WorkloadKeyId kid,
                                          String publicKeyPem, Instant notBefore, Instant notAfter,
                                          TrustAdministrationActor actor, TrustAdministrationReason reason,
                                          Instant now);

    PrincipalSnapshot lockPrincipal(WorkloadPrincipalId principalId);

    CredentialSnapshot lockCredential(UUID principalDatabaseId, WorkloadKeyId kid);

    void updatePrincipalLifecycle(PrincipalSnapshot principal, WorkloadPrincipalLifecycle target, Instant now);

    void updateCredentialLifecycle(CredentialSnapshot credential, WorkloadCredentialLifecycle target, Instant now);

    void appendAudit(UUID principalDatabaseId, UUID credentialDatabaseId, TrustAdministrationActor actor,
                     TrustAdministrationReason reason, String action, Map<String, Object> metadata, Instant now);

    enum RegistrationResult { CREATED, NO_OP }

    record PrincipalSnapshot(UUID id, String principalId, String issuer, WorkloadPrincipalLifecycle lifecycle,
                             Instant disabledAt, Instant revokedAt) { }

    record CredentialSnapshot(UUID id, UUID principalId, String kid, WorkloadCredentialLifecycle lifecycle,
                              Instant activatedAt, Instant revokedAt, Instant retiredAt) { }
}
