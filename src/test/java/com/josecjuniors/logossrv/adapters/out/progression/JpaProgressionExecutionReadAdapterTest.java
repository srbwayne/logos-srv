package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionReadCorruptedException;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JpaProgressionExecutionReadAdapterTest {
    private final ProgressionExternalExecutionJpaRepository repository = mock(ProgressionExternalExecutionJpaRepository.class);
    private final JpaProgressionExecutionReadAdapter adapter = new JpaProgressionExecutionReadAdapter(repository, new ObjectMapper());

    @Test
    void mapsCompletedOutcomeAndUsesNormalizedIdentityWithoutMutatingEntity() throws Exception {
        var entity = entity(" LIFEOS ", " Session-1 ", "COMPLETED", outcomeJson());
        when(repository.findBySourceSystemAndIdempotencyKey("lifeos", "Session-1"))
                .thenReturn(java.util.Optional.of(entity));

        var read = adapter.find(new ProgressionExecutionIdentity(" LIFEOS ", " Session-1 ")).orElseThrow();

        assertThat(read.identity().source()).isEqualTo("lifeos");
        assertThat(read.identity().idempotencyKey()).isEqualTo("Session-1");
        assertThat(read.status()).isEqualTo(ProgressionExecutionStatus.COMPLETED);
        assertThat(read.outcome().result().xpGlobal()).isEqualTo(120);
        verify(repository).findBySourceSystemAndIdempotencyKey("lifeos", "Session-1");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void doesNotDeserializeResponseForNonCompletedExecution() {
        var entity = entity("lifeos", "pending", "PENDING", "not-an-outcome");
        when(repository.findBySourceSystemAndIdempotencyKey("lifeos", "pending"))
                .thenReturn(java.util.Optional.of(entity));

        var read = adapter.find(new ProgressionExecutionIdentity("lifeos", "pending")).orElseThrow();

        assertThat(read.status()).isEqualTo(ProgressionExecutionStatus.PENDING);
        assertThat(read.outcome()).isNull();
        verify(repository).findBySourceSystemAndIdempotencyKey("lifeos", "pending");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsCorruptedCompletedOutcomeWithSafeTypedException() {
        var entity = entity("lifeos", "corrupt", "COMPLETED", "not-an-outcome");
        when(repository.findBySourceSystemAndIdempotencyKey("lifeos", "corrupt"))
                .thenReturn(java.util.Optional.of(entity));

        assertThatThrownBy(() -> adapter.find(new ProgressionExecutionIdentity("lifeos", "corrupt")))
                .isInstanceOf(ProgressionExecutionReadCorruptedException.class)
                .hasMessage("Progression execution data is corrupted.");
        verify(repository).findBySourceSystemAndIdempotencyKey("lifeos", "corrupt");
        verifyNoMoreInteractions(repository);
    }

    private ProgressionExternalExecutionEntity entity(String source, String key, String status, String response) {
        var entity = mock(ProgressionExternalExecutionEntity.class);
        when(entity.getSourceSystem()).thenReturn(source);
        when(entity.getIdempotencyKey()).thenReturn(key);
        when(entity.getSubjectNamespace()).thenReturn("lifeos");
        when(entity.getSubjectExternalId()).thenReturn("user-1");
        when(entity.getConfigurationKey()).thenReturn("reading");
        when(entity.getRequestedRevision()).thenReturn(2);
        when(entity.getConfigurationVersionId()).thenReturn(UUID.randomUUID());
        when(entity.getSkillPolicyVersionId()).thenReturn(UUID.randomUUID());
        when(entity.getProcessingStatus()).thenReturn(status);
        when(entity.getResponseJson()).thenReturn(response);
        return entity;
    }

    private String outcomeJson() throws Exception {
        return new ObjectMapper().writeValueAsString(new ProgressionOutcome(
                new ProgressionResult(120, 4, List.of()),
                new ProgressionProfile(120, 1, 4, 1, List.of(), List.of())));
    }
}
