package com.josecjuniors.logossrv.core.subjectownership.application.service;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.subjectownership.application.port.in.TransferExternalSubjectOwnershipCommand;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransferExternalSubjectOwnershipServiceTest {
    private final SubjectOwnershipOperatorContext authority = mock(SubjectOwnershipOperatorContext.class);
    private final SubjectOwnershipLifecycleStore store = mock(SubjectOwnershipLifecycleStore.class);
    private final TransferExternalSubjectOwnershipService service =
            new TransferExternalSubjectOwnershipService(authority, store);
    private final ExternalSubjectReference reference = new ExternalSubjectReference("lifeos", "subject-1");
    private final UUID targetA = UUID.randomUUID();
    private final UUID targetB = UUID.randomUUID();
    private final AuthorizedSubjectOwnershipOperator operator =
            new AuthorizedSubjectOwnershipOperator(PrincipalType.WORKLOAD, "trusted-workload", "lifeos");

    private TransferExternalSubjectOwnershipCommand command(long version, UUID target, String evidence, String reason) {
        return new TransferExternalSubjectOwnershipCommand(reference, version, target, evidence, reason);
    }

    private SubjectOwnershipAggregate aggregate(OwnershipStatus ownership, VerificationStatus verification, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), reference, targetA, IdentityClass.EXTERNAL,
                ownership, verification, version);
    }

    @Test void authorizesExactNamespaceBeforeLookupAndSavesNormalizedTransferWithFixedEvidenceType() {
        var before = aggregate(OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 4);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(before));
        when(store.targetExists(targetB)).thenReturn(true);
        service.transfer(command(4, targetB, "  case-17  ", "  accepted by both parties  "));

        var order = inOrder(authority, store);
        order.verify(authority).authorizeForNamespace("lifeos");
        order.verify(store).findForUpdate(reference);
        order.verify(store).isEquivalentTransferReplay(eq(before), eq(4L), eq(targetB),
                argThat(evidence -> evidence.reference().equals("case-17")),
                argThat(reason -> reason.value().equals("accepted by both parties")));
        order.verify(store).targetExists(targetB);
        verify(store).saveTransfer(eq(before), argThat(after -> after.id().equals(before.id())
                        && after.reference().equals(reference) && after.targetJogadorId().equals(targetB)
                        && after.identityClass() == IdentityClass.EXTERNAL
                        && after.ownershipStatus() == OwnershipStatus.ACTIVE
                        && after.verificationStatus() == VerificationStatus.UNVERIFIED
                        && after.ownershipVersion() == 5), eq(operator),
                argThat(evidence -> evidence.reference().equals("case-17")),
                argThat(reason -> reason.value().equals("accepted by both parties")), any());
    }

    @Test void deniedAuthorityAndNotFoundDoNotProceedToTransferPersistence() {
        when(authority.authorizeForNamespace("lifeos")).thenThrow(new IllegalStateException("denied"))
                .thenReturn(operator);
        assertThatThrownBy(() -> service.transfer(command(0, targetB, "evidence", "reason")))
                .isInstanceOf(IllegalStateException.class);
        verify(store, never()).findForUpdate(any());
        when(store.findForUpdate(reference)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transfer(command(0, targetB, "evidence", "reason")))
                .isInstanceOf(SubjectOwnershipIdentityNotFoundException.class);
        verify(store, never()).saveTransfer(any(), any(), any(), any(), any(), any());
    }

    @Test void validatesReasonAndEvidenceBeforeReplayAndReturnsWithoutAnyWrite() {
        var current = new SubjectOwnershipAggregate(UUID.randomUUID(), reference, targetB,
                IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED, 5);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current));
        when(store.isEquivalentTransferReplay(eq(current), eq(4L), eq(targetB), any(), any())).thenReturn(true);
        service.transfer(command(4, targetB, " evidence ", " reason "));
        assertThatThrownBy(() -> service.transfer(command(4, targetB, " ", " reason ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transfer(command(4, targetB, " evidence ", " ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).targetExists(any());
        verify(store, never()).saveTransfer(any(), any(), any(), any(), any(), any());
    }

    @Test void staleRealTransferConflictsAndMissingTargetDoesNotPersist() {
        var current = aggregate(OwnershipStatus.DISABLED, VerificationStatus.INVALIDATED, 9);
        when(authority.authorizeForNamespace("lifeos")).thenReturn(operator);
        when(store.findForUpdate(reference)).thenReturn(Optional.of(current));
        when(store.targetExists(targetB)).thenReturn(false);
        assertThatThrownBy(() -> service.transfer(command(8, targetB, "case", "reason")))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
        assertThatThrownBy(() -> service.transfer(command(9, targetB, "case", "reason")))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class)
                .hasMessageContaining("does not exist");
        verify(store, never()).saveTransfer(any(), any(), any(), any(), any(), any());
    }

    @Test void commandHasOnlyAuthorizedInputFieldsAndEvidenceTypeIsFixedInternally() {
        assertThat(TransferExternalSubjectOwnershipCommand.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("reference", "expectedOwnershipVersion", "newTargetJogadorId",
                        "transferEvidenceReference", "reason");
        assertThat(SubjectOwnershipTransferEvidence.TYPE).isEqualTo("BILATERAL_TRANSFER_CONSENT");
    }
}
