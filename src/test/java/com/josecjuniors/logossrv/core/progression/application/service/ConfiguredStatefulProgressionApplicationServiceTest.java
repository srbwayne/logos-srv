package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfiguration;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredStatefulProgressionApplicationServiceTest {

    @Test
    void resolveConfiguracaoMontaInputEDelegaAoFluxoStateful() {
        var subject = new SubjectId(UUID.randomUUID());
        var reference = new ProgressionConfigurationReference(UUID.randomUUID());
        var profile = new ProgressionProfile(0, 1, 0, 1, List.of(), List.of());
        var repository = new RecordingRepository(profile, profile);
        var resolver = (ProgressionConfigurationResolver) ignored -> Optional.of(
                new ProgressionConfiguration(10, 0, List.of(), List.of()));
        var delegate = new StatefulProgressionApplicationService(repository,
                (value, current) -> new ProgressionOutcome(
                        new com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult(0, 0, List.of()), current));
        var service = new ConfiguredStatefulProgressionApplicationService(
                repository, resolver, new ProgressionInputFactory(), delegate);

        var outcome = service.execute(subject, reference, new ProgressionFact(List.of()));

        assertThat(repository.savedSubject).isEqualTo(subject);
        assertThat(repository.savedProfile).isSameAs(profile);
        assertThat(outcome.updatedProfile()).isSameAs(profile);
    }

    @Test
    void retornaNotFoundQuandoConfiguracaoNaoExiste() {
        var repository = new RecordingRepository(new ProgressionProfile(0, 1, 0, 1, List.of(), List.of()), null);
        var resolver = (ProgressionConfigurationResolver) ignored -> Optional.empty();
        var delegate = new StatefulProgressionApplicationService(repository,
                (value, current) -> new ProgressionOutcome(
                        new com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult(0, 0, List.of()), current));
        var service = new ConfiguredStatefulProgressionApplicationService(
                repository, resolver, new ProgressionInputFactory(), delegate);

        assertThatThrownBy(() -> service.execute(new SubjectId(UUID.randomUUID()),
                new ProgressionConfigurationReference(UUID.randomUUID()), new ProgressionFact(List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    private static final class RecordingRepository implements ProgressionProfileRepository {
        private final ProgressionProfile current;
        private final ProgressionProfile saved;
        private SubjectId savedSubject;
        private ProgressionProfile savedProfile;

        private RecordingRepository(ProgressionProfile current, ProgressionProfile saved) {
            this.current = current;
            this.saved = saved;
        }

        @Override
        public Optional<ProgressionProfile> findBySubjectId(SubjectId subjectId) {
            return Optional.ofNullable(current);
        }

        @Override
        public Optional<ProgressionProfile> findBySubjectIdForUpdate(SubjectId subjectId) {
            return Optional.ofNullable(current);
        }

        @Override
        public ProgressionProfile save(SubjectId subjectId, ProgressionProfile profile) {
            savedSubject = subjectId;
            savedProfile = profile;
            return saved;
        }
    }

}
