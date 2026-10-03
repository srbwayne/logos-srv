package com.josecjuniors.logossrv.core.subjectownership.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectOwnershipClassificationTest {

    @Test
    void classifiesLegacyNativeWithoutClaimingRecoverableOrigin() {
        assertThat(SubjectOwnershipClassification.legacy("logos-native"))
                .isEqualTo(new SubjectOwnershipClassification(IdentityClass.LOGOS_NATIVE,
                        OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                        OwnershipProvenance.LEGACY_LOGOS_NATIVE_UNKNOWN));
    }

    @Test
    void classifiesLegacyExternalAsUnverifiedWithUnknownOrigin() {
        assertThat(SubjectOwnershipClassification.legacy("lifeos"))
                .isEqualTo(new SubjectOwnershipClassification(IdentityClass.EXTERNAL,
                        OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED,
                        OwnershipProvenance.LEGACY_EXTERNAL_UNKNOWN));
    }

    @Test
    void distinguishesProspectiveNativeRegistrationAndPocSelfLink() {
        assertThat(SubjectOwnershipClassification.nativeRegistration().provenance())
                .isEqualTo(OwnershipProvenance.LOGOS_NATIVE_REGISTRATION);
        assertThat(SubjectOwnershipClassification.pocSelfLink())
                .isEqualTo(new SubjectOwnershipClassification(IdentityClass.EXTERNAL,
                        OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED,
                        OwnershipProvenance.POC_SELF_LINK));
    }

    @Test
    void refusesUnverifiedOrExternalNativeClassification() {
        assertThatThrownBy(() -> new SubjectOwnershipClassification(IdentityClass.LOGOS_NATIVE,
                OwnershipStatus.ACTIVE, VerificationStatus.UNVERIFIED,
                OwnershipProvenance.LOGOS_NATIVE_REGISTRATION))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubjectOwnershipClassification(IdentityClass.EXTERNAL,
                OwnershipStatus.ACTIVE, VerificationStatus.NOT_REQUIRED,
                OwnershipProvenance.POC_SELF_LINK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
