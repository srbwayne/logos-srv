package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.InvalidSubjectOwnershipTransitionException;
import com.josecjuniors.logossrv.core.subjectownership.domain.exception.SubjectOwnershipVersionConflictException;
import java.util.Objects;
import java.util.UUID;

public record SubjectOwnershipAggregate(UUID id, ExternalSubjectReference reference, UUID targetJogadorId,
        IdentityClass identityClass, OwnershipStatus ownershipStatus, VerificationStatus verificationStatus,
        long ownershipVersion) {
    public SubjectOwnershipAggregate {
        Objects.requireNonNull(id); Objects.requireNonNull(reference); Objects.requireNonNull(targetJogadorId);
        Objects.requireNonNull(identityClass); Objects.requireNonNull(ownershipStatus);
        Objects.requireNonNull(verificationStatus);
        if (ownershipVersion < 0) throw new IllegalArgumentException("ownershipVersion must be nonnegative");
    }

    public boolean isActiveVerified() {
        return identityClass == IdentityClass.EXTERNAL && ownershipStatus == OwnershipStatus.ACTIVE
                && verificationStatus == VerificationStatus.VERIFIED
                && !reference.namespace().equals("logos-native");
    }

    public SubjectOwnershipAggregate verify(long expectedVersion) {
        if (reference.namespace().equals("logos-native") || identityClass != IdentityClass.EXTERNAL)
            throw new InvalidSubjectOwnershipTransitionException("Only external identities may be verified");
        if (ownershipStatus != OwnershipStatus.ACTIVE)
            throw new InvalidSubjectOwnershipTransitionException("Only active ownership may be verified");
        if (verificationStatus != VerificationStatus.UNVERIFIED)
            throw new InvalidSubjectOwnershipTransitionException("Current verification state cannot be verified");
        if (ownershipVersion != expectedVersion)
            throw new SubjectOwnershipVersionConflictException(expectedVersion, ownershipVersion);
        return new SubjectOwnershipAggregate(id, reference, targetJogadorId, identityClass, ownershipStatus,
                VerificationStatus.VERIFIED, ownershipVersion + 1);
    }
}
