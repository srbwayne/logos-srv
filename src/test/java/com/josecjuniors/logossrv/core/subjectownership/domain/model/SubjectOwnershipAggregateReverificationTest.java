package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateReverificationTest {
    private SubjectOwnershipAggregate invalidated() {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "subject-1"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 12);
    }

    @Test void reverifiesWithoutChangingIdentityLocatorTargetOrOwnership() {
        var before = invalidated();
        var after = before.reverify(12);
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.reference()).isEqualTo(before.reference());
        assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
        assertThat(after.identityClass()).isEqualTo(IdentityClass.EXTERNAL);
        assertThat(after.ownershipStatus()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(after.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(after.ownershipVersion()).isEqualTo(13);
    }

    @Test void rejectsNativeAndUnsupportedStates() {
        var nativeIdentity = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "subject"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0);
        assertThatThrownBy(() -> nativeIdentity.reverify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        var nonExternal = new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("lifeos", "subject"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.INVALIDATED, 0);
        assertThatThrownBy(() -> nonExternal.reverify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);

        for (OwnershipStatus status : new OwnershipStatus[]{OwnershipStatus.DISABLED, OwnershipStatus.REVOKED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "subject"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, status, VerificationStatus.INVALIDATED, 0);
            assertThatThrownBy(() -> current.reverify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }

        for (VerificationStatus status : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                VerificationStatus.NOT_REQUIRED, VerificationStatus.VERIFIED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "subject"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, status, 0);
            assertThatThrownBy(() -> current.reverify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
    }

    @Test void rejectsStaleVersionForRealTransition() {
        assertThatThrownBy(() -> invalidated().reverify(11)).isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }
}
