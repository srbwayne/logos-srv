package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class JpaExternalSubjectResolverPostgresIT {

    @Autowired
    private JpaExternalSubjectResolver resolver;

    @Test
    void resolveMappingBackfillEmPostgresRetornaSubjectInterno() {
        var subjectId = resolver.resolve(new ExternalSubjectReference(
                " LOGOS-NATIVE ", "10000000-0000-0000-0000-000000000001"));

        assertThat(subjectId.value())
                .isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    }
}
