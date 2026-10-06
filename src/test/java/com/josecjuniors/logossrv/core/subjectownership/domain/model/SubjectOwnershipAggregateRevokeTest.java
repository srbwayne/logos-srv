package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateRevokeTest {
    @Test void revokesActiveAndDisabledAcrossEverySupportedVerificationState() {
        for (var status : new OwnershipStatus[]{OwnershipStatus.ACTIVE, OwnershipStatus.DISABLED}) {
            for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                    VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
                var before = aggregate(status, verification, IdentityClass.EXTERNAL, "lifeos");
                var after = before.revoke(11);
                assertThat(after.id()).isEqualTo(before.id());
                assertThat(after.reference()).isEqualTo(before.reference());
                assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
                assertThat(after.identityClass()).isEqualTo(before.identityClass());
                assertThat(after.ownershipStatus()).isEqualTo(OwnershipStatus.REVOKED);
                assertThat(after.verificationStatus()).isEqualTo(verification);
                assertThat(after.ownershipVersion()).isEqualTo(12);
            }
        }
    }

    @Test void replayRecognizesOnlySupportedExternalRevokedStates() {
        for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
            assertThat(aggregate(OwnershipStatus.REVOKED, verification, IdentityClass.EXTERNAL, "lifeos")
                    .isExternalRevokedWithSupportedVerification()).isTrue();
        }
        assertThat(aggregate(OwnershipStatus.REVOKED, VerificationStatus.NOT_REQUIRED,
                IdentityClass.EXTERNAL, "lifeos").isExternalRevokedWithSupportedVerification()).isFalse();
        assertThat(aggregate(OwnershipStatus.REVOKED, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "logos-native").isExternalRevokedWithSupportedVerification()).isFalse();
    }

    @Test void rejectsNativeNotRequiredUnsupportedSourcesAndStaleVersion() {
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                IdentityClass.EXTERNAL, "lifeos"));
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "logos-native"));
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                IdentityClass.LOGOS_NATIVE, "logos-native"));
        assertInvalid(aggregate(OwnershipStatus.REVOKED, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "lifeos"));
        assertThatThrownBy(() -> aggregate(OwnershipStatus.DISABLED, VerificationStatus.INVALIDATED,
                IdentityClass.EXTERNAL, "lifeos").revoke(10))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }

    @Test void reactivationStillRejectsRevokedBinding() {
        assertThatThrownBy(() -> aggregate(OwnershipStatus.REVOKED, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "lifeos").reactivate(11))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }

    private SubjectOwnershipAggregate aggregate(OwnershipStatus status, VerificationStatus verification,
            IdentityClass identityClass, String namespace) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference(namespace, "subject"),
                UUID.randomUUID(), identityClass, status, verification, 11);
    }

    private void assertInvalid(SubjectOwnershipAggregate aggregate) {
        assertThatThrownBy(() -> aggregate.revoke(aggregate.ownershipVersion()))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }
}
