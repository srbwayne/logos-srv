package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.RevokeExternalSubjectOwnershipCommand;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RevokeExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final RevokeExternalSubjectOwnershipService service = new RevokeExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference reference = new ExternalSubjectReference("lifeos", "subject-1");
    private final AuthorizedSubjectOwnershipOperator operator =
            new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "trusted-workload-1", "lifeos");

    private RevokeExternalSubjectOwnershipCommand command(long version) {
        return new RevokeExternalSubjectOwnershipCommand(reference, version, "  legal request  ");
    }

    private SubjectOwnershipAggregate identity(IdentityClass identityClass, OwnershipStatus ownership,
            VerificationStatus verification, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), reference, UUID.randomUUID(), identityClass,
                ownership, verification, version);
    }

    @Test void authorizesExactNamespaceBeforeLookupAndPersistsTrustedActorAndReason() {
        var before = identity(IdentityClass.EXTERNAL, OwnershipStatus.DISABLED, VerificationStatus.INVALIDATED, 4);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(before));
        service.revoke(command(4));
        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        verify(store).saveRevoke(eq(before), argThat(after -> after.ownershipStatus() == OwnershipStatus.REVOKED
                        && after.verificationStatus() == VerificationStatus.INVALIDATED
                        && after.ownershipVersion() == 5), eq(operator),
                argThat(reason -> reason.value().equals("legal request")), any());
    }

    @Test void deniedAuthorityDoesNotDiscloseIdentityAndMissingIdentityFollowsAuthorization() {
        when(authority.authorizeForNamespace("lifeos")).thenThrow(new IllegalStateException("denied"))
                .thenReturn(operator);
        assertThatThrownBy(() -> service.revoke(command(0))).isInstanceOf(IllegalStateException.class);
        verify(store, never()).findForUpdate(any());
        when(store.findForUpdate(reference)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(command(0))).isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        verify(store, never()).saveRevoke(any(), any(), any(), any(), any());
    }

    @Test void revokedReplayIgnoresStaleVersionButValidatesReasonBeforeReturning() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(identity(IdentityClass.EXTERNAL,
                OwnershipStatus.REVOKED, VerificationStatus.VERIFIED, 9)));
        service.revoke(command(0));
        assertThatThrownBy(() -> service.revoke(new RevokeExternalSubjectOwnershipCommand(reference, 0, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).saveRevoke(any(), any(), any(), any(), any());
    }

    @Test void unsupportedRevokedShapeIsNotReplayAndRealTransitionChecksExpectedVersion() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(identity(IdentityClass.EXTERNAL,
                OwnershipStatus.REVOKED, VerificationStatus.NOT_REQUIRED, 9)))
                .thenReturn(Optional.of(identity(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE,
                        VerificationStatus.VERIFIED, 9)));
        assertThatThrownBy(() -> service.revoke(command(0))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        assertThatThrownBy(() -> service.revoke(command(8))).isInstanceOf(SubjectOwnershipVersionConflictException.class);
        verify(store, never()).saveRevoke(any(), any(), any(), any(), any());
    }

    @Test void commandDoesNotAcceptAnActorField() {
        assertThat(RevokeExternalSubjectOwnershipCommand.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("reference", "expectedOwnershipVersion", "reason");
    }
}
