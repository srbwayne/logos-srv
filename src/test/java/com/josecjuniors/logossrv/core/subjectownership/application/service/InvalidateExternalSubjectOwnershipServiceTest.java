package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.InvalidateExternalSubjectOwnershipCommand;
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

class InvalidateExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final InvalidateExternalSubjectOwnershipService service = new InvalidateExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference ref = new ExternalSubjectReference("lifeos", "user-1");
    private final AuthorizedSubjectOwnershipOperator operator = new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "workload-1", "lifeos");
    private InvalidateExternalSubjectOwnershipCommand command(long version) {
        return new InvalidateExternalSubjectOwnershipCommand(ref, version, " ticket ", " case-1 ", " reviewed ");
    }
    private SubjectOwnershipAggregate current(VerificationStatus status, OwnershipStatus ownership, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), ref, UUID.randomUUID(), IdentityClass.EXTERNAL, ownership, status, version);
    }

    @Test void authorizesExactNamespaceBeforeLookupAndPersistsTrustedOperator() {
        var before = current(VerificationStatus.VERIFIED, OwnershipStatus.ACTIVE, 4);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(before));
        service.invalidate(command(4));
        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(ref);
        verify(store).saveInvalidation(eq(before), argThat(after -> after.ownershipVersion() == 5
                && after.verificationStatus() == VerificationStatus.INVALIDATED), eq(operator),
                argThat(e -> e.evidenceType().equals("ticket") && e.evidenceReference().equals("case-1")
                        && e.reason().equals("reviewed")), any());
    }

    @Test void missingIdentityIsDisclosedOnlyAfterAuthorization() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.invalidate(command(0))).isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(ref);
        verify(store, never()).saveInvalidation(any(), any(), any(), any(), any());
    }

    @Test void alreadyInvalidatedIgnoresStaleVersionButStillValidatesEvidence() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(current(VerificationStatus.INVALIDATED, OwnershipStatus.ACTIVE, 9)));
        service.invalidate(command(0));
        verify(store, never()).saveInvalidation(any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service.invalidate(new InvalidateExternalSubjectOwnershipCommand(ref, 0, " ", "r", "reason")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).saveInvalidation(any(), any(), any(), any(), any());
    }

    @Test void staleVersionAndInvalidStateDoNotPersist() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(current(VerificationStatus.VERIFIED, OwnershipStatus.ACTIVE, 4)))
                .thenReturn(Optional.of(current(VerificationStatus.UNVERIFIED, OwnershipStatus.ACTIVE, 4)));
        assertThatThrownBy(() -> service.invalidate(command(3))).isInstanceOf(SubjectOwnershipVersionConflictException.class);
        assertThatThrownBy(() -> service.invalidate(command(4))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        verify(store, never()).saveInvalidation(any(), any(), any(), any(), any());
    }
}
