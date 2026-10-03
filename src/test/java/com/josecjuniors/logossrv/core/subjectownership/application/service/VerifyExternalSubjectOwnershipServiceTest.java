package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.VerifyExternalSubjectOwnershipCommand;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipLifecycleStore;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipIdentityNotFoundException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import com.josecjuniors.logossrv.core.subjectownership.domain.model.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VerifyExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final VerifyExternalSubjectOwnershipService service = new VerifyExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference ref = new ExternalSubjectReference("lifeos", "user-1");
    private final AuthorizedSubjectOwnershipOperator operator = new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "workload-1", "lifeos");
    private final VerifyExternalSubjectOwnershipCommand command = new VerifyExternalSubjectOwnershipCommand(ref, 0, "ticket", "e-1", "reviewed");
    private SubjectOwnershipAggregate current(VerificationStatus status, OwnershipStatus ownership, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), ref, UUID.randomUUID(), IdentityClass.EXTERNAL, ownership, status, version);
    }

    @Test void authorizesBeforeLookupAndPersistsTrustedActorAndTransition() {
        var current = current(VerificationStatus.UNVERIFIED, OwnershipStatus.ACTIVE, 0);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(current));
        service.verify(command);
        verify(authority).authorizeForNamespace("lifeos");
        verify(store).saveVerification(eq(current), argThat(after -> after.ownershipVersion() == 1
                && after.verificationStatus() == VerificationStatus.VERIFIED), eq(operator), any(), any());
    }

    @Test void missingIdentityIsExplicitAfterAuthorization() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify(command)).isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        verify(store, never()).saveVerification(any(), any(), any(), any(), any());
    }

    @Test void staleVersionIsExplicit() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(current(VerificationStatus.UNVERIFIED, OwnershipStatus.ACTIVE, 4)));
        assertThatThrownBy(() -> service.verify(command)).isInstanceOf(SubjectOwnershipVersionConflictException.class);
        verify(store, never()).saveVerification(any(), any(), any(), any(), any());
    }

    @Test void alreadyVerifiedIsNoOpBeforeVersionAndEvidenceValidation() {
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(ref)).thenReturn(Optional.of(current(VerificationStatus.VERIFIED, OwnershipStatus.ACTIVE, 9)));
        service.verify(new VerifyExternalSubjectOwnershipCommand(ref, 0, null, null, null));
        verify(store, never()).saveVerification(any(), any(), any(), any(), any());
    }
}
