package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import java.util.Objects;

/** Initial ownership classification for an existing or newly provisioned locator. */
public record SubjectOwnershipClassification(
        IdentityClass identityClass,
        OwnershipStatus ownershipStatus,
        VerificationStatus verificationStatus,
        OwnershipProvenance provenance) {

    private static final String LOGOS_NATIVE_NAMESPACE = "logos-native";

    public SubjectOwnershipClassification {
        Objects.requireNonNull(identityClass, "identityClass");
        Objects.requireNonNull(ownershipStatus, "ownershipStatus");
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        Objects.requireNonNull(provenance, "provenance");
        if (identityClass == IdentityClass.LOGOS_NATIVE
                && (ownershipStatus != OwnershipStatus.ACTIVE
                || verificationStatus != VerificationStatus.NOT_REQUIRED
                || (provenance != OwnershipProvenance.LEGACY_LOGOS_NATIVE_UNKNOWN
                && provenance != OwnershipProvenance.LOGOS_NATIVE_REGISTRATION))) {
            throw new IllegalArgumentException("Invalid initial logos-native classification");
        }
        if (identityClass == IdentityClass.EXTERNAL
                && (ownershipStatus != OwnershipStatus.ACTIVE
                || verificationStatus != VerificationStatus.UNVERIFIED
                || (provenance != OwnershipProvenance.LEGACY_EXTERNAL_UNKNOWN
                && provenance != OwnershipProvenance.POC_SELF_LINK))) {
            throw new IllegalArgumentException("Invalid initial external classification");
        }
    }

    public static SubjectOwnershipClassification legacy(String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        return namespace.equals(LOGOS_NATIVE_NAMESPACE)
                ? new SubjectOwnershipClassification(IdentityClass.LOGOS_NATIVE, OwnershipStatus.ACTIVE,
                VerificationStatus.NOT_REQUIRED, OwnershipProvenance.LEGACY_LOGOS_NATIVE_UNKNOWN)
                : new SubjectOwnershipClassification(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE,
                VerificationStatus.UNVERIFIED, OwnershipProvenance.LEGACY_EXTERNAL_UNKNOWN);
    }

    public static SubjectOwnershipClassification nativeRegistration() {
        return new SubjectOwnershipClassification(IdentityClass.LOGOS_NATIVE, OwnershipStatus.ACTIVE,
                VerificationStatus.NOT_REQUIRED, OwnershipProvenance.LOGOS_NATIVE_REGISTRATION);
    }

    public static SubjectOwnershipClassification pocSelfLink() {
        return new SubjectOwnershipClassification(IdentityClass.EXTERNAL, OwnershipStatus.ACTIVE,
                VerificationStatus.UNVERIFIED, OwnershipProvenance.POC_SELF_LINK);
    }
}
