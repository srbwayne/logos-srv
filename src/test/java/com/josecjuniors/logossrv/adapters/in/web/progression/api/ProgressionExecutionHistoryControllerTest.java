package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.in.web.exception.GlobalExceptionHandler;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionHistoryQuery;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteIdempotentExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.in.GetProgressionExecutionQuery;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryItem;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionReadCorruptedException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

class ProgressionExecutionHistoryControllerTest {
    private final GetProgressionExecutionQuery exactQuery = mock(GetProgressionExecutionQuery.class);
    private final GetProgressionExecutionHistoryQuery historyQuery = mock(GetProgressionExecutionHistoryQuery.class);
    private final ExecuteIdempotentExternalSubjectProgressionUseCase executionUseCase = mock(ExecuteIdempotentExternalSubjectProgressionUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgressionExecutionController(exactQuery, historyQuery, executionUseCase))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsHistoryItemWithNullableOccurredAtAndNoPersistenceInternals() throws Exception {
        when(historyQuery.get(any(), any())).thenReturn(page(read(ProgressionExecutionStatus.FAILED), null));

        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", " LIFEOS ")
                        .queryParam("subjectExternalId", " user-1 ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].identity.source").value("lifeos"))
                .andExpect(jsonPath("$.items[0].subject.externalId").value("user-1"))
                .andExpect(jsonPath("$.items[0].requestedRevision").value(2))
                .andExpect(jsonPath("$.items[0].occurredAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].outcome").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.items[0].requestJson").doesNotExist())
                .andExpect(jsonPath("$.items[0].requestFingerprint").doesNotExist())
                .andExpect(jsonPath("$.items[0].lastError").doesNotExist())
                .andExpect(jsonPath("$.items[0].attemptCount").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void serializesCanonicalOccurredAtAsUtcInstant() throws Exception {
        when(historyQuery.get(any(), any())).thenReturn(page(read(ProgressionExecutionStatus.COMPLETED),
                Instant.parse("2026-09-17T01:23:45Z")));

        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-09-17T01:23:45Z"));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "user-1")
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "user-1")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsCorruptHistoryToSafeServerError() throws Exception {
        when(historyQuery.get(any(), any())).thenThrow(new ProgressionExecutionReadCorruptedException(
                new IllegalArgumentException("SENSITIVE_RAW_PERSISTENCE_CONTENT")));

        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "user-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("PROGRESSION_EXECUTION_READ_CORRUPTED"))
                .andExpect(jsonPath("$.error").value("Progression execution data is corrupted."))
                .andExpect(content().string(not(containsString("SENSITIVE_RAW_PERSISTENCE_CONTENT"))));
    }

    private ProgressionExecutionHistoryPage page(ProgressionExecutionRead read, Instant occurredAt) {
        return new ProgressionExecutionHistoryPage(List.of(new ProgressionExecutionHistoryItem(read, occurredAt)),
                0, 20, 1, 1, false);
    }

    private ProgressionExecutionRead read(ProgressionExecutionStatus status) {
        return new ProgressionExecutionRead(new ProgressionExecutionIdentity("lifeos", "key-1"),
                new ExternalSubjectReference("lifeos", "user-1"), "reading", 2,
                UUID.randomUUID(), UUID.randomUUID(), status, (ProgressionOutcome) null);
    }
}
