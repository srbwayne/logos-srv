package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.DisableExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DisableExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final DisableExternalSubjectOwnershipService service = new DisableExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference reference = new ExternalSubjectReference("lifeos", "subject-1");
    private final AuthorizedSubjectOwnershipOperator operator =
            new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "trusted-workload-1", "lifeos");

    private DisableExternalSubjectOwnershipCommand command(long version) {
        return new DisableExternalSubjectOwnershipCommand(reference, version, "  administrative review  ");
    }

    private SubjectOwnershipAggregate current(IdentityClass identityClass, OwnershipStatus ownership,
            VerificationStatus verification, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), reference, UUID.randomUUID(), identityClass,
                ownership, verification, version);
    }

    @Test void authorizesExactNamespaceBeforeLookupAndPersistsTrustedOperatorAndNormalizedReason() {
        var before = current(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 4);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(before));

        service.disable(command(4));

        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        verify(store).saveDisable(eq(before), argThat(after -> after.ownershipStatus() == OwnershipStatus.DISABLED
                        && after.verificationStatus() == VerificationStatus.INVALIDATED && after.ownershipVersion() == 5),
                eq(operator), argThat(reason -> reason.value().equals("administrative review")), any());
    }

    @Test void failedAuthorizationDoesNotRevealWhetherIdentityExists() {
        when(authority.authorizeForNamespace("lifeos")).thenThrow(new IllegalStateException("denied"));
        assertThatThrownBy(() -> service.disable(command(0))).isInstanceOf(IllegalStateException.class);
        verify(store, never()).findForUpdate(any());
        verify(store, never()).saveDisable(any(), any(), any(), any(), any());
    }

    @Test void notFoundIsReportedOnlyAfterAuthorization() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.disable(command(0))).isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        verify(store, never()).saveDisable(any(), any(), any(), any(), any());
    }

    @Test void disabledReplayIgnoresStaleVersionButValidatesReasonAndDoesNotPersist() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current(
                IdentityClass.EXTERNAL, OwnershipStatus.DISABLED, VerificationStatus.VERIFIED, 9)));
        service.disable(command(0));
        assertThatThrownBy(() -> service.disable(new DisableExternalSubjectOwnershipCommand(reference, 0, " ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).saveDisable(any(), any(), any(), any(), any());
    }

    @Test void unsupportedDisabledShapeAndStaleRealTransitionFailWithoutPersistence() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current(
                        IdentityClass.EXTERNAL, OwnershipStatus.DISABLED, VerificationStatus.NOT_REQUIRED, 9)))
                .thenReturn(Optional.of(current(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 9)));
        assertThatThrownBy(() -> service.disable(command(0))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> service.disable(command(8))).isInstanceOf(SubjectOwnershipVersionConflictException.class);
        verify(store, never()).saveDisable(any(), any(), any(), any(), any());
    }
}
