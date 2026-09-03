package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ExternalSubjectResolver;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalSubjectProgressionApplicationServiceTest {

    @Test
    void resolveIdentidadeEDelegaSemDuplicarProgressao() {
        var internalSubject = new SubjectId(UUID.randomUUID());
        var reference = new ExternalSubjectReference("lifeos", "A");
        var resolver = (ExternalSubjectResolver) ignored -> internalSubject;
        var expected = new ProgressionOutcome(
                new com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult(1, 2, List.of()),
                null);
        var delegate = (ExecuteConfiguredSubjectProgressionUseCase) (subject, configuration, fact) -> {
            assertThat(subject).isEqualTo(internalSubject);
            return expected;
        };

        var result = new ExternalSubjectProgressionApplicationService(resolver, delegate).execute(
                reference, new ProgressionConfigurationReference(UUID.randomUUID()), new ProgressionFact(List.of()));

        assertThat(result).isSameAs(expected);
    }
}
