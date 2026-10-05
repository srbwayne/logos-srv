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

    public boolean isActiveInvalidated() {
        return identityClass == IdentityClass.EXTERNAL && ownershipStatus == OwnershipStatus.ACTIVE
                && verificationStatus == VerificationStatus.INVALIDATED
                && !reference.namespace().equals("logos-native");
    }

    public boolean isExternalDisabled() {
        return identityClass == IdentityClass.EXTERNAL && !reference.namespace().equals("logos-native")
                && ownershipStatus == OwnershipStatus.DISABLED
                && (verificationStatus == VerificationStatus.UNVERIFIED
                        || verificationStatus == VerificationStatus.VERIFIED
                        || verificationStatus == VerificationStatus.INVALIDATED);
    }

    public boolean isExternalActiveWithSupportedVerification() {
        return identityClass == IdentityClass.EXTERNAL && !reference.namespace().equals("logos-native")
                && ownershipStatus == OwnershipStatus.ACTIVE
                && (verificationStatus == VerificationStatus.UNVERIFIED
                        || verificationStatus == VerificationStatus.VERIFIED
                        || verificationStatus == VerificationStatus.INVALIDATED);
    }

    public SubjectOwnershipAggregate disable(long expectedVersion) {
        if (reference.namespace().equals("logos-native") || identityClass != IdentityClass.EXTERNAL)
            throw new InvalidSubjectOwnershipTransitionException("Only external identities may be disabled");
        if (ownershipStatus != OwnershipStatus.ACTIVE)
            throw new InvalidSubjectOwnershipTransitionException("Only active ownership may be disabled");
        if (verificationStatus != VerificationStatus.UNVERIFIED
                && verificationStatus != VerificationStatus.VERIFIED
                && verificationStatus != VerificationStatus.INVALIDATED)
            throw new InvalidSubjectOwnershipTransitionException("Current verification state cannot be disabled");
        if (ownershipVersion != expectedVersion)
            throw new SubjectOwnershipVersionConflictException(expectedVersion, ownershipVersion);
        return new SubjectOwnershipAggregate(id, reference, targetJogadorId, identityClass, OwnershipStatus.DISABLED,
                verificationStatus, ownershipVersion + 1);
    }

    public SubjectOwnershipAggregate reactivate(long expectedVersion) {
        if (reference.namespace().equals("logos-native") || identityClass != IdentityClass.EXTERNAL)
            throw new InvalidSubjectOwnershipTransitionException("Only external identities may be reactivated");
        if (ownershipStatus != OwnershipStatus.DISABLED)
            throw new InvalidSubjectOwnershipTransitionException("Only disabled ownership may be reactivated");
        if (verificationStatus != VerificationStatus.UNVERIFIED
                && verificationStatus != VerificationStatus.VERIFIED
                && verificationStatus != VerificationStatus.INVALIDATED)
            throw new InvalidSubjectOwnershipTransitionException("Current verification state cannot be reactivated");
        if (ownershipVersion != expectedVersion)
            throw new SubjectOwnershipVersionConflictException(expectedVersion, ownershipVersion);
        return new SubjectOwnershipAggregate(id, reference, targetJogadorId, identityClass, OwnershipStatus.ACTIVE,
                verificationStatus, ownershipVersion + 1);
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

    public SubjectOwnershipAggregate invalidate(long expectedVersion) {
        if (reference.namespace().equals("logos-native") || identityClass != IdentityClass.EXTERNAL)
            throw new InvalidSubjectOwnershipTransitionException("Only external identities may be invalidated");
        if (ownershipStatus != OwnershipStatus.ACTIVE)
            throw new InvalidSubjectOwnershipTransitionException("Only active ownership may be invalidated");
        if (verificationStatus != VerificationStatus.VERIFIED)
            throw new InvalidSubjectOwnershipTransitionException("Only verified ownership may be invalidated");
        if (ownershipVersion != expectedVersion)
            throw new SubjectOwnershipVersionConflictException(expectedVersion, ownershipVersion);
        return new SubjectOwnershipAggregate(id, reference, targetJogadorId, identityClass, ownershipStatus,
                VerificationStatus.INVALIDATED, ownershipVersion + 1);
    }

    public SubjectOwnershipAggregate reverify(long expectedVersion) {
        if (reference.namespace().equals("logos-native") || identityClass != IdentityClass.EXTERNAL)
            throw new InvalidSubjectOwnershipTransitionException("Only external identities may be reverified");
        if (ownershipStatus != OwnershipStatus.ACTIVE)
            throw new InvalidSubjectOwnershipTransitionException("Only active ownership may be reverified");
        if (verificationStatus != VerificationStatus.INVALIDATED)
            throw new InvalidSubjectOwnershipTransitionException("Only invalidated ownership may be reverified");
        if (ownershipVersion != expectedVersion)
            throw new SubjectOwnershipVersionConflictException(expectedVersion, ownershipVersion);
        return new SubjectOwnershipAggregate(id, reference, targetJogadorId, identityClass, ownershipStatus,
                VerificationStatus.VERIFIED, ownershipVersion + 1);
    }
}
