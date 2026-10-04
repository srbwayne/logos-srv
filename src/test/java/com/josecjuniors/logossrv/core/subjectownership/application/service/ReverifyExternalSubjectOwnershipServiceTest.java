package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.ReverifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.SubjectOwnershipAggregate;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReverifyExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final ReverifyExternalSubjectOwnershipService service = new ReverifyExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference reference = new ExternalSubjectReference("lifeos", "subject-1");
    private final AuthorizedSubjectOwnershipOperator operator =
            new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "trusted-workload-1", "lifeos");

    private ReverifyExternalSubjectOwnershipCommand command(long version) {
        return new ReverifyExternalSubjectOwnershipCommand(reference, version, " evidence-review ", " opaque-ref ", " reviewed ");
    }

    private SubjectOwnershipAggregate current(IdentityClass identityClass, OwnershipStatus ownership,
            VerificationStatus verification, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), reference, UUID.randomUUID(), identityClass,
                ownership, verification, version);
    }

    @Test void authorizesBeforeLookupAndPersistsTrustedOperatorAndNormalizedEvidence() {
        var before = current(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 8);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(before));

        service.reverify(command(8));

        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        verify(store).saveReverification(eq(before), argThat(after -> after.verificationStatus() == VerificationStatus.VERIFIED
                        && after.ownershipVersion() == 9), eq(operator),
                argThat(evidence -> evidence.evidenceType().equals("evidence-review")
                        && evidence.evidenceReference().equals("opaque-ref") && evidence.reason().equals("reviewed")), any());
    }

    @Test void notFoundIsReturnedOnlyAfterAuthorization() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reverify(command(0))).isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        verify(store, never()).saveReverification(any(), any(), any(), any(), any());
    }

    @Test void alreadyVerifiedIsNoOpWithStaleVersionButStillValidatesEvidence() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current(
                IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 9)));
        service.reverify(command(0));
        assertThatThrownBy(() -> service.reverify(new ReverifyExternalSubjectOwnershipCommand(reference, 0, " ", "ref", "reason")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).saveReverification(any(), any(), any(), any(), any());
    }

    @Test void realTransitionRejectsStaleVersionAndUnsupportedVerifiedShape() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current(
                        IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 9)))
                .thenReturn(Optional.of(current(IdentityClass.EXTERNAL, OwnershipStatus.DISABLED, VerificationStatus.VERIFIED, 9)));
        assertThatThrownBy(() -> service.reverify(command(8))).isInstanceOf(SubjectOwnershipVersionConflictException.class);
        assertThatThrownBy(() -> service.reverify(command(9))).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        verify(store, never()).saveReverification(any(), any(), any(), any(), any());
    }
}
