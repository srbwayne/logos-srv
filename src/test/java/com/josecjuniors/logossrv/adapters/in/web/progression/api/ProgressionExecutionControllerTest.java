package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.exception.GlobalExceptionHandler;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.response.ProgressionExecutionReadResponse;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionReadCorruptedException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class ProgressionExecutionControllerTest {
    private final GetProgressionExecutionQuery query = mock(GetProgressionExecutionQuery.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgressionExecutionController(query))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTypedCompletedExecutionWithoutPersistenceInternals() throws Exception {
        var configurationVersionId = UUID.randomUUID();
        var skillPolicyVersionId = UUID.randomUUID();
        var outcome = new ProgressionOutcome(
                new ProgressionResult(120, 4, List.of(new ProgressionResult.AttributeProgression("forca", 80))),
                new ProgressionProfile(120, 1, 4, 1,
                        List.of(new ProgressionProfile.ProgressionAttribute("forca", 80, 1)), List.of()));
        when(query.get(any())).thenReturn(read(ProgressionExecutionStatus.COMPLETED, outcome,
                configurationVersionId, skillPolicyVersionId));

        mockMvc.perform(get("/api/internal/v3/progression/executions")
                        .queryParam("sourceSystem", "  LIFEOS ")
                        .queryParam("idempotencyKey", "  Session-1  ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.source").value("lifeos"))
                .andExpect(jsonPath("$.identity.idempotencyKey").value("Session-1"))
                .andExpect(jsonPath("$.subject.namespace").value("lifeos"))
                .andExpect(jsonPath("$.subject.externalId").value("user-1"))
                .andExpect(jsonPath("$.configurationKey").value("reading"))
                .andExpect(jsonPath("$.requestedRevision").value(2))
                .andExpect(jsonPath("$.configurationVersionId").value(configurationVersionId.toString()))
                .andExpect(jsonPath("$.skillPolicyVersionId").value(skillPolicyVersionId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.outcome.result.globalXpDelta").value(120))
                .andExpect(jsonPath("$.outcome.profile.globalXp").value(120))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.requestJson").doesNotExist())
                .andExpect(jsonPath("$.requestFingerprint").doesNotExist())
                .andExpect(jsonPath("$.lastError").doesNotExist())
                .andExpect(jsonPath("$.attemptCount").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        var identity = org.mockito.ArgumentCaptor.forClass(ProgressionExecutionIdentity.class);
        verify(query).get(identity.capture());
        assertThat(identity.getValue().source()).isEqualTo("lifeos");
        assertThat(identity.getValue().idempotencyKey()).isEqualTo("Session-1");
    }

    @Test
    void preservesNullableRequestedRevisionAsExplicitJsonNull() throws Exception {
        when(query.get(any())).thenReturn(read(ProgressionExecutionStatus.COMPLETED, null,
                UUID.randomUUID(), UUID.randomUUID(), null));

        mockMvc.perform(get("/api/internal/v3/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "nullable-revision"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"requestedRevision\":null")))
                .andExpect(jsonPath("$.requestedRevision").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.identity.source").value("lifeos"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.outcome").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void returnsNullOutcomeForEveryNonCompletedStatus() throws Exception {
        for (var statusValue : List.of(ProgressionExecutionStatus.PENDING,
                ProgressionExecutionStatus.PROCESSING, ProgressionExecutionStatus.FAILED)) {
            when(query.get(any())).thenReturn(read(statusValue, null, UUID.randomUUID(), UUID.randomUUID()));

            mockMvc.perform(get("/api/internal/v3/progression/executions")
                            .queryParam("sourceSystem", "lifeos")
                            .queryParam("idempotencyKey", statusValue.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(statusValue.name()))
                    .andExpect(jsonPath("$.outcome").isEmpty());
        }
    }

    @Test
    void mapsUnknownExecutionToStableNotFoundContract() throws Exception {
        when(query.get(any())).thenThrow(new ProgressionExecutionNotFoundException());

        mockMvc.perform(get("/api/internal/v3/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROGRESSION_EXECUTION_NOT_FOUND"));
    }

    @Test
    void mapsCorruptedCompletedExecutionToSafeInternalServerError() throws Exception {
        var corrupted = new ProgressionExecutionReadCorruptedException(
                new IllegalArgumentException("SENSITIVE_RAW_PERSISTENCE_CONTENT"));
        when(query.get(any())).thenThrow(corrupted);

        mockMvc.perform(get("/api/internal/v3/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "corrupt"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Progression execution data is corrupted."))
                .andExpect(jsonPath("$.code").value("PROGRESSION_EXECUTION_READ_CORRUPTED"))
                .andExpect(content().string(not(containsString("SENSITIVE_RAW_PERSISTENCE_CONTENT"))))
                .andExpect(content().string(not(containsString("IllegalArgumentException"))));
    }

    private ProgressionExecutionRead read(ProgressionExecutionStatus status, ProgressionOutcome outcome,
                                          UUID configurationVersionId, UUID skillPolicyVersionId) {
        return read(status, outcome, configurationVersionId, skillPolicyVersionId, 2);
    }

    private ProgressionExecutionRead read(ProgressionExecutionStatus status, ProgressionOutcome outcome,
                                          UUID configurationVersionId, UUID skillPolicyVersionId,
                                          Integer requestedRevision) {
        return new ProgressionExecutionRead(
                new ProgressionExecutionIdentity("lifeos", "Session-1"),
                new ExternalSubjectReference("lifeos", "user-1"), "reading", requestedRevision,
                configurationVersionId, skillPolicyVersionId, status, outcome);
    }
}
