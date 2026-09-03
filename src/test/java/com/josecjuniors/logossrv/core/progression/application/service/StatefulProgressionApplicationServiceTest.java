package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionInput;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatefulProgressionApplicationServiceTest {

    @Test
    void carregaExecutaSalvaEDevolveOutcomeDoSubject() {
        var subject = new SubjectId(UUID.randomUUID());
        var current = new ProgressionProfile(0, 1, 0, 1, List.of(), List.of());
        var input = new ProgressionInput(0, 0, List.of(), List.of(), List.of());
        var saved = new ProgressionProfile(10, 1, 0, 1, List.of(), List.of());
        var repository = new RecordingRepository(current, saved);
        var delegate = new RecordingUseCase(saved);
        var service = new StatefulProgressionApplicationService(repository, delegate);

        ProgressionOutcome outcome = service.execute(subject, input);

        assertThat(delegate.subjectProfile).isSameAs(current);
        assertThat(delegate.input).isSameAs(input);
        assertThat(repository.savedSubject).isEqualTo(subject);
        assertThat(repository.savedProfile).isSameAs(saved);
        assertThat(outcome.updatedProfile()).isSameAs(saved);
        assertThat(outcome.result().xpGlobal()).isZero();
    }

    @Test
    void falhaQuandoSubjectNaoPossuiEstado() {
        var repository = new RecordingRepository(null, null);
        var service = new StatefulProgressionApplicationService(repository, (input, profile) -> null);

        assertThatThrownBy(() -> service.execute(
                new SubjectId(UUID.randomUUID()),
                new ProgressionInput(0, 0, List.of(), List.of(), List.of())))
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
        public ProgressionProfile save(SubjectId subjectId, ProgressionProfile profile) {
            savedSubject = subjectId;
            savedProfile = profile;
            return saved;
        }
    }

    private static final class RecordingUseCase implements ExecuteProgressionUseCase {
        private final ProgressionProfile resultProfile;
        private ProgressionInput input;
        private ProgressionProfile subjectProfile;

        private RecordingUseCase(ProgressionProfile resultProfile) {
            this.resultProfile = resultProfile;
        }

        @Override
        public ProgressionOutcome execute(ProgressionInput input, ProgressionProfile currentProfile) {
            this.input = input;
            this.subjectProfile = currentProfile;
            return new ProgressionOutcome(new ProgressionResult(0, 0, List.of()), resultProfile);
        }
    }
}
