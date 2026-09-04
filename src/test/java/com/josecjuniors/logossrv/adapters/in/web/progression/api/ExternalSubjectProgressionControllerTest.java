package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.exception.GlobalExceptionHandler;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionEvaluationRequest;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteExternalSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExternalSubjectProgressionControllerTest {

    private final ExecuteExternalSubjectProgressionUseCase useCase = mock(ExecuteExternalSubjectProgressionUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExternalSubjectProgressionController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void avaliaPorIdentidadeExternaEReutilizaResponse() throws Exception {
        UUID configurationId = UUID.randomUUID();
        var outcome = new ProgressionOutcome(
                new ProgressionResult(120, 4, List.of(new ProgressionResult.AttributeProgression("forca", 80))),
                new ProgressionProfile(120, 1, 4, 1,
                        List.of(new ProgressionProfile.ProgressionAttribute("forca", 80, 1)), List.of()));
        when(useCase.execute(any(), any(), any())).thenReturn(outcome);

        mockMvc.perform(post("/api/internal/v1/progression/external/{namespace}/{externalId}/evaluate",
                        "EXPERIMENT", "subject-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProgressionEvaluationRequest(configurationId,
                                        List.of(new ProgressionEvaluationRequest.DetailRequest("pages", 120))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.globalXpDelta").value(120))
                .andExpect(jsonPath("$.profile.globalXp").value(120));

        var subject = org.mockito.ArgumentCaptor.forClass(ExternalSubjectReference.class);
        verify(useCase).execute(subject.capture(), any(ProgressionConfigurationReference.class), any(ProgressionFact.class));
        assertThat(subject.getValue()).isEqualTo(new ExternalSubjectReference("experiment", "subject-001"));
    }

    @Test
    void mappingAusenteRetorna404() throws Exception {
        when(useCase.execute(any(), any(), any())).thenThrow(new ProgressionSubjectNotFoundException());

        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/missing/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void configuracaoAusenteRetorna404() throws Exception {
        when(useCase.execute(any(), any(), any())).thenThrow(new ProgressionConfigurationNotFoundException());

        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/subject-001/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void namespaceInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/external/{namespace}/subject-001/evaluate", "a".repeat(65))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void externalIdInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/{externalId}/evaluate", "a".repeat(256))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bodyInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/subject-001/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configurationId\":null,\"details\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void externalIdMantemCaseSensitivityDoMapping() throws Exception {
        when(useCase.execute(any(), any(), any())).thenThrow(new ProgressionSubjectNotFoundException());

        mockMvc.perform(post("/api/internal/v1/progression/external/experiment/abc123/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    private ProgressionEvaluationRequest validRequest() {
        return new ProgressionEvaluationRequest(UUID.randomUUID(), List.of());
    }
}
