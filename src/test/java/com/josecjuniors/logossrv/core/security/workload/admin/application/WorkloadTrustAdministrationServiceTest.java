package com.josecjuniors.logossrv.core.security.workload.admin.application;

import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationError;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.CredentialSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.PrincipalSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActorType;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkloadTrustAdministrationServiceTest {
    private static final TrustAdministrationActor ACTOR =
            new TrustAdministrationActor(TrustAdministrationActorType.OPERATOR, "test-operator");
    private static final TrustAdministrationReason REASON = new TrustAdministrationReason("test reason");
    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05.123456789Z");

    private WorkloadTrustAdministrationStore store;
    private WorkloadTrustAdministrationService service;

    @BeforeEach
    void setUp() {
        store = mock(WorkloadTrustAdministrationStore.class);
        service = new WorkloadTrustAdministrationService(store, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void normalizesCredentialWindowBeforeValidatingAndPassingToStore() {
        Instant before = Instant.parse("2026-01-02T03:04:05.123456100Z");
        Instant after = Instant.parse("2026-01-02T03:04:06.123456900Z");
        var command = new WorkloadTrustAdministrationService.RegisterCredentialCommand(
                new WorkloadPrincipalId("lifeos"), new WorkloadKeyId("kid-a"), "pem", before, after, ACTOR, REASON);

        service.registerCredential(command);

        verify(store).registerCredential(eq(new WorkloadPrincipalId("lifeos")), eq(new WorkloadKeyId("kid-a")),
                eq("pem"), eq(Instant.parse("2026-01-02T03:04:05.123456Z")),
                eq(Instant.parse("2026-01-02T03:04:06.123456Z")), eq(ACTOR), eq(REASON),
                eq(Instant.parse("2026-01-02T03:04:05.123456Z")));
    }

    @Test
    void rejectsWindowThatCollapsesAtMicrosecondPrecisionBeforeStoreCall() {
        var command = new WorkloadTrustAdministrationService.RegisterCredentialCommand(
                new WorkloadPrincipalId("lifeos"), new WorkloadKeyId("kid-a"), "pem",
                Instant.parse("2026-01-02T03:04:05.123456100Z"),
                Instant.parse("2026-01-02T03:04:05.123456900Z"), ACTOR, REASON);

        assertThatThrownBy(() -> service.registerCredential(command))
                .isInstanceOf(TrustAdministrationException.class)
                .extracting(error -> ((TrustAdministrationException) error).error())
                .isEqualTo(TrustAdministrationError.INVALID_VALIDITY_WINDOW);
        verify(store, never()).registerCredential(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void samePrincipalLifecycleIsAnAuditFreeNoOp() {
        var snapshot = new PrincipalSnapshot(UUID.randomUUID(), "lifeos", "issuer", WorkloadPrincipalLifecycle.REVOKED,
                null, Instant.parse("2026-01-02T03:00:00Z"));
        when(store.lockPrincipal(new WorkloadPrincipalId("lifeos"))).thenReturn(snapshot);

        service.changePrincipalLifecycle(new WorkloadTrustAdministrationService.PrincipalLifecycleCommand(
                new WorkloadPrincipalId("lifeos"), WorkloadPrincipalLifecycle.REVOKED, ACTOR, REASON));

        verify(store, never()).updatePrincipalLifecycle(any(), any(), any());
        verify(store, never()).appendAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void revokedPrincipalCannotBeReenabled() {
        var snapshot = new PrincipalSnapshot(UUID.randomUUID(), "lifeos", "issuer", WorkloadPrincipalLifecycle.REVOKED,
                null, NOW);
        when(store.lockPrincipal(new WorkloadPrincipalId("lifeos"))).thenReturn(snapshot);

        assertThatThrownBy(() -> service.changePrincipalLifecycle(
                new WorkloadTrustAdministrationService.PrincipalLifecycleCommand(
                        new WorkloadPrincipalId("lifeos"), WorkloadPrincipalLifecycle.ACTIVE, ACTOR, REASON)))
                .isInstanceOf(TrustAdministrationException.class)
                .extracting(error -> ((TrustAdministrationException) error).error())
                .isEqualTo(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
        verify(store, never()).updatePrincipalLifecycle(any(), any(), any());
    }

    @Test
    void revokedCredentialCannotBeRetiredOrReactivated() {
        UUID principalPk = UUID.randomUUID();
        when(store.lockPrincipal(any())).thenReturn(new PrincipalSnapshot(principalPk, "lifeos", "issuer",
                WorkloadPrincipalLifecycle.ACTIVE, null, null));
        when(store.lockCredential(eq(principalPk), any())).thenReturn(new CredentialSnapshot(UUID.randomUUID(),
                principalPk, "kid-a", WorkloadCredentialLifecycle.REVOKED, null, NOW, null));

        for (WorkloadCredentialLifecycle target : new WorkloadCredentialLifecycle[]{
                WorkloadCredentialLifecycle.ACTIVE, WorkloadCredentialLifecycle.RETIRED}) {
            assertThatThrownBy(() -> service.changeCredentialLifecycle(
                    new WorkloadTrustAdministrationService.CredentialLifecycleCommand(
                            new WorkloadPrincipalId("lifeos"), new WorkloadKeyId("kid-a"), target, ACTOR, REASON)))
                    .isInstanceOf(TrustAdministrationException.class)
                    .extracting(error -> ((TrustAdministrationException) error).error())
                    .isEqualTo(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
        }
        verify(store, never()).updateCredentialLifecycle(any(), any(), any());
    }
}
