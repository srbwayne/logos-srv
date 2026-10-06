package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipAggregateTransferTest {
    private final UUID targetA = UUID.randomUUID();
    private final UUID targetB = UUID.randomUUID();

    @Test void preservesIdentityLocatorClassAndOwnershipAcrossAllSixSupportedTransfers() {
        for (var ownership : new OwnershipStatus[]{OwnershipStatus.ACTIVE, OwnershipStatus.DISABLED}) {
            for (var verification : new VerificationStatus[]{VerificationStatus.UNVERIFIED,
                    VerificationStatus.VERIFIED, VerificationStatus.INVALIDATED}) {
                var before = aggregate(ownership, verification, IdentityClass.EXTERNAL, "lifeos", targetA);
                var after = before.transfer(7, targetB);
                assertThat(after.id()).isEqualTo(before.id());
                assertThat(after.reference()).isEqualTo(before.reference());
                assertThat(after.identityClass()).isEqualTo(IdentityClass.EXTERNAL);
                assertThat(after.ownershipStatus()).isEqualTo(ownership);
                assertThat(after.targetJogadorId()).isEqualTo(targetB);
                assertThat(after.verificationStatus()).isEqualTo(verification == VerificationStatus.VERIFIED
                        ? VerificationStatus.UNVERIFIED : verification);
                assertThat(after.ownershipVersion()).isEqualTo(8);
            }
        }
    }

    @Test void rejectsNativeRevokedNotRequiredSameTargetMissingTargetAndStaleVersion() {
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                IdentityClass.EXTERNAL, "lifeos", targetA), targetB, 7);
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "logos-native", targetA), targetB, 7);
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                IdentityClass.LOGOS_NATIVE, "logos-native", targetA), targetB, 7);
        assertInvalid(aggregate(OwnershipStatus.REVOKED, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "lifeos", targetA), targetB, 7);
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "lifeos", targetA), targetA, 7);
        assertInvalid(aggregate(OwnershipStatus.ACTIVE, VerificationStatus.VERIFIED,
                IdentityClass.EXTERNAL, "lifeos", targetA), null, 7);
        assertThatThrownBy(() -> aggregate(OwnershipStatus.DISABLED, VerificationStatus.INVALIDATED,
                IdentityClass.EXTERNAL, "lifeos", targetA).transfer(6, targetB))
                .isInstanceOf(SubjectOwnershipVersionConflictException.class);
    }

    @Test void rejectsTransferAtVersionOverflow() {
        assertThatThrownBy(() -> aggregate(OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED,
                IdentityClass.EXTERNAL, "lifeos", targetA, Long.MAX_VALUE).transfer(Long.MAX_VALUE, targetB))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }

    private SubjectOwnershipAggregate aggregate(OwnershipStatus ownership, VerificationStatus verification,
            IdentityClass identityClass, String namespace, UUID target) {
        return aggregate(ownership, verification, identityClass, namespace, target, 7);
    }

    private SubjectOwnershipAggregate aggregate(OwnershipStatus ownership, VerificationStatus verification,
            IdentityClass identityClass, String namespace, UUID target, long version) {
        return new SubjectOwnershipAggregate(UUID.randomUUID(), new ExternalSubjectReference(namespace, "subject"),
                target, identityClass, ownership, verification, version);
    }

    private void assertInvalid(SubjectOwnershipAggregate aggregate, UUID target, long version) {
        assertThatThrownBy(() -> aggregate.transfer(version, target))
                .isInstanceOf(InvalidSubjectOwnershipTransitionException.class);
    }
}
