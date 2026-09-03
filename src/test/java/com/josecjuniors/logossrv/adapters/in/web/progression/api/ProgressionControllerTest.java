package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.in.web.exception.GlobalExceptionHandler;
import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionEvaluationRequest;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteConfiguredSubjectProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionConfigurationNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProgressionControllerTest {

    private final ExecuteConfiguredSubjectProgressionUseCase useCase = mock(ExecuteConfiguredSubjectProgressionUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgressionController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void avaliaProgressaoComApenasReferenciaEFatos() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();
        var outcome = new ProgressionOutcome(
                new ProgressionResult(120, 4, List.of(new ProgressionResult.AttributeProgression("forca", 80))),
                new ProgressionProfile(120, 1, 4, 1,
                        List.of(new ProgressionProfile.ProgressionAttribute("forca", 80, 1)), List.of()));
        when(useCase.execute(any(), any(), any())).thenReturn(outcome);

        var request = new ProgressionEvaluationRequest(configurationId,
                List.of(new ProgressionEvaluationRequest.DetailRequest("pages", 120)));
        mockMvc.perform(post("/api/internal/v1/progression/{subjectId}/evaluate", subjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.globalXpDelta").value(120))
                .andExpect(jsonPath("$.result.stressTotal").value(4))
                .andExpect(jsonPath("$.profile.globalXp").value(120))
                .andExpect(jsonPath("$.profile.attributes[0].key").value("forca"));

        var subjectCaptor = org.mockito.ArgumentCaptor.forClass(SubjectId.class);
        var referenceCaptor = org.mockito.ArgumentCaptor.forClass(ProgressionConfigurationReference.class);
        var factCaptor = org.mockito.ArgumentCaptor.forClass(ProgressionFact.class);
        verify(useCase).execute(subjectCaptor.capture(), referenceCaptor.capture(), factCaptor.capture());
        assertThat(subjectCaptor.getValue().value()).isEqualTo(subjectId);
        assertThat(referenceCaptor.getValue().value()).isEqualTo(configurationId);
        assertThat(factCaptor.getValue().details()).extracting(ProgressionFact.Detail::factorKey)
                .containsExactly("pages");
    }

    @Test
    void converteSubjectInexistentePara404() throws Exception {
        when(useCase.execute(any(), any(), any())).thenThrow(new ProgressionSubjectNotFoundException());

        mockMvc.perform(post("/api/internal/v1/progression/{subjectId}/evaluate", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void converteConfiguracaoInexistentePara404() throws Exception {
        when(useCase.execute(any(), any(), any())).thenThrow(new ProgressionConfigurationNotFoundException());

        mockMvc.perform(post("/api/internal/v1/progression/{subjectId}/evaluate", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void uuidInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/invalido/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestNaoModelaEstadoOuRegras() {
        assertThat(java.util.Arrays.stream(ProgressionEvaluationRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("configurationId", "details");
    }

    private ProgressionEvaluationRequest validRequest() {
        return new ProgressionEvaluationRequest(UUID.randomUUID(), List.of());
    }
}
