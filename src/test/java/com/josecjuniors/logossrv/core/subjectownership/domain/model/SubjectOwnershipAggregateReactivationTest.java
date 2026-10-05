package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateReactivationTest {
    private SubjectOwnershipAggregate disabled(VerificationStatus verification) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "subject-1"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.DISABLED, verification, 11);
    }

    @Test void reactivatesEverySupportedVerificationStateAndPreservesIdentity() {
        for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
            var before = disabled(verification);
            var after = before.reactivate(11);
            assertThat(after.id()).isEqualTo(before.id());
            assertThat(after.reference()).isEqualTo(before.reference());
            assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
            assertThat(after.identityClass()).isEqualTo(IdentityClass.EXTERNAL);
            assertThat(after.ownershipStatus()).isEqualTo(OwnershipStatus.ACTIVE);
            assertThat(after.verificationStatus()).isEqualTo(verification);
            assertThat(after.ownershipVersion()).isEqualTo(12);
        }
    }

    @Test void recognizesOnlySupportedExternalActiveStatesAsReplay() {
        for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
            assertThat(new SubjectOwnershipAggregate(UUID.randomUUID(),
                    new ExternalSubjectReference("lifeos", "subject-1"), UUID.randomUUID(), IdentityClass.EXTERNAL,
                    OwnershipStatus.ACTIVE, verification, 12).isExternalActiveWithSupportedVerification()).isTrue();
        }
        assertThat(active(IdentityClass.EXTERNAL, "lifeos", VerificationStatus.NOT_REQUIRED)
                .isExternalActiveWithSupportedVerification()).isFalse();
        assertThat(active(IdentityClass.LOGOS_NATIVE, "logos-native", VerificationStatus.NOT_REQUIRED)
                .isExternalActiveWithSupportedVerification()).isFalse();
        assertThat(active(IdentityClass.EXTERNAL, "logos-native", VerificationStatus.VERIFIED)
                .isExternalActiveWithSupportedVerification()).isFalse();
    }

    @Test void rejectsNativeWrongOwnershipNotRequiredAndRevokedStates() {
        assertInvalid(active(IdentityClass.LOGOS_NATIVE, "logos-native", VerificationStatus.NOT_REQUIRED));
        assertInvalid(active(IdentityClass.EXTERNAL, "logos-native", VerificationStatus.VERIFIED));
        assertInvalid(new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                UUID.randomUUID(), IdentityClass.LOGOS_NATIVE, OwnershipStatus.DISABLED,
                VerificationStatus.VERIFIED, 11));
        assertInvalid(new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.REVOKED,
                VerificationStatus.VERIFIED, 11));
        assertInvalid(disabled(VerificationStatus.NOT_REQUIRED));
    }

    @Test void rejectsStaleExpectedVersionForRealTransition() {
        assertThatThrownBy(() -> disabled(VerificationStatus.INVALIDATED).reactivate(10))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }

    private SubjectOwnershipAggregate active(IdentityClass identityClass, String namespace,
            VerificationStatus verification) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference(namespace, "subject-1"),
                UUID.randomUUID(), identityClass, OwnershipStatus.ACTIVE, verification, 12);
    }

    private void assertInvalid(SubjectOwnershipAggregate aggregate) {
        assertThatThrownBy(() -> aggregate.reactivate(aggregate.ownershipVersion()))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }
}
