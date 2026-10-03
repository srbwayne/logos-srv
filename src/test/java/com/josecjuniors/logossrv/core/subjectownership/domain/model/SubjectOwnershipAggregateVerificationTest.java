package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateVerificationTest {
    private SubjectOwnershipAggregate activeUnverified() {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "user-1"),
                UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED, 7);
    }

    @Test void verifiesWithoutChangingIdentityOwnershipOrTarget() {
        var before = activeUnverified();
        var after = before.verify(7);
        assertThat(after.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(after.ownershipVersion()).isEqualTo(8);
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.reference()).isEqualTo(before.reference());
        assertThat(after.targetJogadorId()).isEqualTo(before.targetJogadorId());
        assertThat(after.identityClass()).isEqualTo(before.identityClass());
        assertThat(after.ownershipStatus()).isEqualTo(before.ownershipStatus());
    }

    @Test void rejectsNativeAndNonActiveOrUnsupportedVerificationStates() {
        assertThatThrownBy(() -> new SubjectOwnershipAggregate(UUID.randomUUID(),
                new ExternalSubjectReference("logos-native", "x"), UUID.randomUUID(), IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED, 0).verify(0))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        for (OwnershipStatus status : new OwnershipStatus[]{OwnershipStatus.DISABLED, OwnershipStatus.REVOKED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, status, VerificationStatus.UNVERIFIED, 0);
            assertThatThrownBy(() -> current.verify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
        for (VerificationStatus status : new VerificationStatus[]{VerificationStatus.INVALIDATED, VerificationStatus.NOT_REQUIRED,
                VerificationStatus.VERIFIED}) {
            var current = new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference("lifeos", "x"),
                    UUID.randomUUID(), IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE, status, 0);
            assertThatThrownBy(() -> current.verify(0)).isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
        }
    }

    @Test void reportsStaleVersionExplicitly() {
        assertThatThrownBy(() -> activeUnverified().verify(6))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }

    @Test void validatesAndNormalizesEvidence() {
        assertThat(new OwnershipVerificationEvidence("  ticket ", " ref-1 ", " confirmed "))
                .isEqualTo(new OwnershipVerificationEvidence("ticket", "ref-1", "confirmed"));
        assertThatThrownBy(() -> new OwnershipVerificationEvidence(" ", "x", "r"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OwnershipVerificationEvidence("x", " ", "r"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OwnershipVerificationEvidence("x", "r", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OwnershipVerificationEvidence("x".repeat(65), "r", "r"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OwnershipVerificationEvidence("x", "r".repeat(256), "r"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OwnershipVerificationEvidence("x", "r", "r".repeat(513)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
