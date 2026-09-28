package com.josecjuniors.logossrv.core.security.workload.admin.application;

import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationError;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.CredentialSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.PrincipalSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.RegistrationResult;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@Service
public class WorkloadTrustAdministrationService {
    private final WorkloadTrustAdministrationStore store;
    private final Clock clock;

    public WorkloadTrustAdministrationService(WorkloadTrustAdministrationStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public RegistrationResult registerPrincipal(RegisterPrincipalCommand command) {
        Objects.requireNonNull(command, "command");
        return store.registerPrincipal(command.principalId(), command.issuer(), command.actor(), command.reason(), now());
    }

    @Transactional
    public RegistrationResult registerCredential(RegisterCredentialCommand command) {
        Objects.requireNonNull(command, "command");
        Instant notBefore = normalize(command.notBefore());
        Instant notAfter = command.notAfter() == null ? null : normalize(command.notAfter());
        if (notAfter != null && !notAfter.isAfter(notBefore)) {
            throw new TrustAdministrationException(TrustAdministrationError.INVALID_VALIDITY_WINDOW);
        }
        Instant now = now();
        return store.registerCredential(command.principalId(), command.kid(), command.publicKeyPem(),
                notBefore, notAfter, command.actor(), command.reason(), now);
    }

    @Transactional
    public void changePrincipalLifecycle(PrincipalLifecycleCommand command) {
        Objects.requireNonNull(command, "command");
        PrincipalSnapshot principal = store.lockPrincipal(command.principalId());
        WorkloadPrincipalLifecycle current = principal.lifecycle();
        WorkloadPrincipalLifecycle target = command.target();
        if (current == target) return;
        String action = principalAction(current, target);
        Instant now = now();
        store.updatePrincipalLifecycle(principal, target, now);
        store.appendAudit(principal.id(), null, command.actor(), command.reason(), action,
                Map.of("principalType", "WORKLOAD", "principalId", principal.principalId(),
                        "issuer", principal.issuer(), "oldLifecycle", current.name(), "newLifecycle", target.name()), now);
    }

    @Transactional
    public void changeCredentialLifecycle(CredentialLifecycleCommand command) {
        Objects.requireNonNull(command, "command");
        PrincipalSnapshot principal = store.lockPrincipal(command.principalId());
        CredentialSnapshot credential = store.lockCredential(principal.id(), command.kid());
        WorkloadCredentialLifecycle current = credential.lifecycle();
        WorkloadCredentialLifecycle target = command.target();
        if (current == target) return;
        String action = credentialAction(current, target);
        if (target == WorkloadCredentialLifecycle.ACTIVE
                && principal.lifecycle() == WorkloadPrincipalLifecycle.REVOKED) {
            throw new TrustAdministrationException(TrustAdministrationError.PRINCIPAL_REVOKED);
        }
        Instant now = now();
        store.updateCredentialLifecycle(credential, target, now);
        store.appendAudit(principal.id(), credential.id(), command.actor(), command.reason(), action,
                Map.of("principalType", "WORKLOAD", "principalId", principal.principalId(),
                        "issuer", principal.issuer(), "kid", credential.kid(), "algorithm", "ES256",
                        "oldLifecycle", current.name(), "newLifecycle", target.name()), now);
    }

    private static String principalAction(WorkloadPrincipalLifecycle current, WorkloadPrincipalLifecycle target) {
        if (target == WorkloadPrincipalLifecycle.DISABLED && current == WorkloadPrincipalLifecycle.ACTIVE)
            return "WORKLOAD_DISABLED";
        if (target == WorkloadPrincipalLifecycle.ACTIVE && current == WorkloadPrincipalLifecycle.DISABLED)
            return "WORKLOAD_ENABLED";
        if (target == WorkloadPrincipalLifecycle.REVOKED
                && (current == WorkloadPrincipalLifecycle.ACTIVE || current == WorkloadPrincipalLifecycle.DISABLED))
            return "WORKLOAD_REVOKED";
        throw new TrustAdministrationException(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
    }

    private static String credentialAction(WorkloadCredentialLifecycle current, WorkloadCredentialLifecycle target) {
        if (target == WorkloadCredentialLifecycle.ACTIVE && current == WorkloadCredentialLifecycle.PENDING)
            return "CREDENTIAL_ACTIVATED";
        if (target == WorkloadCredentialLifecycle.REVOKED
                && (current == WorkloadCredentialLifecycle.PENDING || current == WorkloadCredentialLifecycle.ACTIVE))
            return "CREDENTIAL_REVOKED";
        if (target == WorkloadCredentialLifecycle.RETIRED && current == WorkloadCredentialLifecycle.ACTIVE)
            return "CREDENTIAL_RETIRED";
        throw new TrustAdministrationException(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static Instant normalize(Instant instant) {
        if (instant == null) throw new TrustAdministrationException(TrustAdministrationError.INVALID_INPUT);
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public record RegisterPrincipalCommand(WorkloadPrincipalId principalId, WorkloadIssuer issuer,
                                           TrustAdministrationActor actor, TrustAdministrationReason reason) {
        public RegisterPrincipalCommand {
            Objects.requireNonNull(principalId, "principalId");
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record RegisterCredentialCommand(WorkloadPrincipalId principalId, WorkloadKeyId kid,
                                            String publicKeyPem, Instant notBefore, Instant notAfter,
                                            TrustAdministrationActor actor, TrustAdministrationReason reason) {
        public RegisterCredentialCommand {
            Objects.requireNonNull(principalId, "principalId");
            Objects.requireNonNull(kid, "kid");
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record PrincipalLifecycleCommand(WorkloadPrincipalId principalId, WorkloadPrincipalLifecycle target,
                                            TrustAdministrationActor actor, TrustAdministrationReason reason) {
        public PrincipalLifecycleCommand {
            Objects.requireNonNull(principalId, "principalId");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record CredentialLifecycleCommand(WorkloadPrincipalId principalId, WorkloadKeyId kid,
                                             WorkloadCredentialLifecycle target, TrustAdministrationActor actor,
                                             TrustAdministrationReason reason) {
        public CredentialLifecycleCommand {
            Objects.requireNonNull(principalId, "principalId");
            Objects.requireNonNull(kid, "kid");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
