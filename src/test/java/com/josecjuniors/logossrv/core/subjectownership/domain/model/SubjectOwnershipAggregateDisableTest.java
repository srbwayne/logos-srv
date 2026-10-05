package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateDisableTest {
    private SubjectOwnershipAggregate active(VerificationStatus verificationStatus) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "subject-1"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, verificationStatus, 7);
    }

    @Test void disablesEverySupportedExternalVerificationStateAndPreservesOtherFields() {
        for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
            var before = active(verification);
            var after = before.disable(7);
            assertThat(after.id()).isEqualTo(before.id());
            assertThat(after.reference()).isEqualTo(before.reference());
            assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
            assertThat(after.identityClass()).isEqualTo(IdentityClass.EXTERNAL);
            assertThat(after.ownershipStatus()).isEqualTo(OwnershipStatus.DISABLED);
            assertThat(after.verificationStatus()).isEqualTo(verification);
            assertThat(after.ownershipVersion()).isEqualTo(8);
        }
    }

    @Test void recognizesOnlySupportedExternalDisabledStatesAsReplay() {
        for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
            var disabled = new SubjectOwnershipAggregate(UUID.randomUUID(),
                    new ExternalSubjectReference("lifeos", "subject-1"), UUID.randomUUID(), IdentityClass.EXTERNAL,
                    OwnershipStatus.DISABLED, verification, 8);
            assertThat(disabled.isExternalDisabled()).isTrue();
        }
        var disabledNotRequired = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("lifeos", "subject-1"), UUID.randomUUID(), IdentityClass.EXTERNAL,
                OwnershipStatus.DISABLED, VerificationStatus.NOT_REQUIRED, 8);
        assertThat(disabledNotRequired.isExternalDisabled()).isFalse();
        var nativeIdentity = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "subject-1"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.DISABLED, VerificationStatus.NOT_REQUIRED, 8);
        assertThat(nativeIdentity.isExternalDisabled()).isFalse();
    }

    @Test void rejectsNativeNonExternalNotRequiredAndUnsupportedOwnershipStates() {
        var nativeIdentity = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "subject-1"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0);
        assertThatThrownBy(() -> nativeIdentity.disable(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        var externalInNativeNamespace = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "subject-1"), UUID.randomUUID(), IdentityClass.EXTERNAL,
                OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 0);
        assertThatThrownBy(() -> externalInNativeNamespace.disable(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        var nonExternal = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                UUID.randomUUID(), IdentityClass.LOGOS_NATIVE, OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 0);
        assertThatThrownBy(() -> nonExternal.disable(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        for (var ownership : new OwnershipStatus[]{OwnershipStatus.DISABLED, OwnershipStatus.REVOKED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, ownership, VerificationStatus.VERIFIED, 0);
            assertThatThrownBy(() -> current.disable(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
        var notRequired = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0);
        assertThatThrownBy(() -> notRequired.disable(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }

    @Test void reportsStaleVersionForRealTransition() {
        assertThatThrownBy(() -> active(VerificationStatus.VERIFIED).disable(6))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }
}
