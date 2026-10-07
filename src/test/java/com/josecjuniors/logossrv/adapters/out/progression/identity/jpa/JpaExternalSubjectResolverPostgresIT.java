package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class JpaExternalSubjectResolverPostgresIT {

    @Autowired
    private JpaExternalSubjectResolver resolver;

    @Autowired
    private ProgressionSubjectIdentityJpaRepository identities;

    @Autowired
    private RegistrationUseCase registration;

    @Test
    void resolveMappingBackfillEmPostgresRetornaSubjectInterno() {
        var registered = registration.register(new RegistrationCommand(
                "resolver-c1a-" + UUID.randomUUID() + "@example.test", "password", "Resolver C1A"));
        var appUserId = registered.user().getId().getValue();
        var subjectId = resolver.resolve(new ExternalSubjectReference(
                " LOGOS-NATIVE ", appUserId.toString()));

        assertThat(subjectId.value())
                .isEqualTo(appUserId);
        var identityId = identities.findCurrentIdentityId("logos-native", appUserId.toString()).orElseThrow();
        var identity = identities.findById(identityId).orElseThrow();
        assertThat(identity.getIdentityClass()).isEqualTo(
                com.josecjuniors.logossrv.core.subjectownership.domain.model.IdentityClass.LOGOS_NATIVE);
        assertThat(identity.getOwnershipStatus()).isEqualTo(
                com.josecjuniors.logossrv.core.subjectownership.domain.model.OwnershipStatus.ACTIVE);
        assertThat(identity.getVerificationStatus()).isEqualTo(
                com.josecjuniors.logossrv.core.subjectownership.domain.model.VerificationStatus.NOT_REQUIRED);
        assertThat(identity.getOwnershipVersion()).isZero();
    }
}
