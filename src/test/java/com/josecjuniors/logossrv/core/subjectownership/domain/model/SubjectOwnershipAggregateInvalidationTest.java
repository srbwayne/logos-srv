package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateInvalidationTest {
    private SubjectOwnershipAggregate activeVerified() {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "user-1"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED, 7);
    }

    @Test void invalidatesWithoutChangingIdentityOwnershipOrTarget() {
        var before = activeVerified();
        var after = before.invalidate(7);
        assertThat(after.verificationStatus()).isEqualTo(VerificationStatus.INVALIDATED);
        assertThat(after.ownershipVersion()).isEqualTo(8);
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.reference()).isEqualTo(before.reference());
        assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
        assertThat(after.identityClass()).isEqualTo(IdentityClass.EXTERNAL);
        assertThat(after.ownershipStatus()).isEqualTo(OwnershipStatus.ACTIVE);
    }

    @Test void recognizesOnlyAnActiveExternalInvalidatedIdentityAsIdempotent() {
        var invalidated = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 8);
        assertThat(invalidated.isActiveInvalidated()).isTrue();
        assertThat(new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("logos-native", "x"),
                UUID.randomUUID(), IdentityClass.LOGOS_NATIVE, OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0)
                .isActiveInvalidated()).isFalse();
    }

    @Test void rejectsNativeAndUnsupportedStates() {
        assertThatThrownBy(() -> new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "x"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0).invalidate(0))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        for (OwnershipStatus status : new OwnershipStatus[]{OwnershipStatus.DISABLED, OwnershipStatus.REVOKED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, status, VerificationStatus.VERIFIED, 0);
            assertThatThrownBy(() -> current.invalidate(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
        for (VerificationStatus status : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.NOT_REQUIRED, VerificationStatus.INVALIDATED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, status, 0);
            assertThatThrownBy(() -> current.invalidate(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
    }

    @Test void reportsStaleVersionForRealTransition() {
        assertThatThrownBy(() -> activeVerified().invalidate(6))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }
}
