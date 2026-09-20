package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FreshPostgresIntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReadingProgressionContractPostgresTest {

    private static final String EMAIL = "reading-poc@example.test";
    private static final String PASSWORD = "reading-poc-password";
    private static final String EXTERNAL_ID = "reading-poc-user";
    private static final String IDEMPOTENCY_KEY = "reading-session-001";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void provesReadingProgressionThroughSupportedLogosHttpContracts() throws Exception {
        JsonNode registration = json(postJson("/api/auth/register", """
                {"email":"%s","password":"%s","nomeExibicao":"Reading POC"}
                """.formatted(EMAIL, PASSWORD)).andExpect(status().isOk()));
        UUID jogadorId = UUID.fromString(registration.get("jogadorId").asText());

        JsonNode login = json(postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD)).andExpect(status().isOk()));
        String token = login.get("token").asText();

        JsonNode attribute = json(authPost(token, "/api/atributos", """
                {"nome":"Conhecimento","descricao":"Conhecimento acumulado por estudo, leitura e aprendizagem.","semanticKey":"knowledge"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Conhecimento"))
                .andExpect(jsonPath("$.semanticKey").value("knowledge")));
        UUID conhecimentoId = UUID.fromString(attribute.get("id").asText());

        JsonNode factor = json(authPost(token, "/api/fatores-calculo", """
                {"semanticKey":"pages_read","nome":"Pages Read","unidadeMedida":"pages","tipoInput":"NUMERICO"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.semanticKey").value("pages_read")));
        assertThat(factor.get("semanticKey").asText()).isEqualTo("pages_read");

        JsonNode definition = json(authPost(token, "/api/progression/configurations", "{\"logicalKey\":\"reading\"}")
                .andExpect(status().isCreated()));
        long draftVersion = definition.get("draftVersion").asLong();
        long activationVersion = definition.get("activationVersion").asLong();

        JsonNode draft = json(mockMvc.perform(put("/api/progression/configurations/reading/draft")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":%d,"baseXp":1,"baseStress":0,"factors":["pages_read"],
                                "distributions":[{"attributeId":"%s","weight":1.0,
                                "xpRules":[{"fact":"pages_read","multiplier":1.0,"minCutoff":null,"maxCutoff":null,"calculationMode":"FACT_VALUE"}],
                                "stressRules":[]}]}
                                """.formatted(draftVersion, conhecimentoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseXp").value(1))
                .andExpect(jsonPath("$.baseStress").value(0))
                .andExpect(jsonPath("$.factors[0]").value("pages_read"))
                .andExpect(jsonPath("$.distributions[0].attributeId").value(conhecimentoId.toString())));

        JsonNode published = json(authPost(token, "/api/progression/configurations/reading/publish", """
                {"expectedDraftVersion":%d}
                """.formatted(draft.get("version").asLong())).andExpect(status().isOk()));
        int revision = published.get("revision").asInt();
        assertThat(revision).isPositive();

        json(authPost(token, "/api/progression/configurations/reading/versions/%d/activate".formatted(revision), """
                {"expectedActivationVersion":%d}
                """.formatted(activationVersion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRevision").value(revision)));

        json(authPost(token, "/api/internal/v1/progression/subject-identities", """
                {"namespace":"lifeos","externalId":"%s"}
                """.formatted(EXTERNAL_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.namespace").value("lifeos"))
                .andExpect(jsonPath("$.externalId").value(EXTERNAL_ID)));

        String executionRequest = executionRequest(revision, 30);
        String first = authPost(token, "/api/internal/v1/progression/executions", executionRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.globalXpDelta").value(30))
                .andExpect(jsonPath("$.result.stressTotal").value(0.0))
                .andExpect(jsonPath("$.result.attributeProgressions[0].key").value(conhecimentoId.toString()))
                .andExpect(jsonPath("$.result.attributeProgressions[0].xp").value(30))
                .andExpect(jsonPath("$.profile.globalXp").value(30))
                .andExpect(jsonPath("$.profile.stress").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat(globalXp(jogadorId)).isEqualTo(30L);
        assertThat(conhecimentoXp(jogadorId, conhecimentoId)).isEqualTo(30L);
        assertThat(executionCount()).isEqualTo(1);

        String replay = authPost(token, "/api/internal/v1/progression/executions", executionRequest)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(replay).isEqualTo(first);
        assertThat(globalXp(jogadorId)).isEqualTo(30L);
        assertThat(conhecimentoXp(jogadorId, conhecimentoId)).isEqualTo(30L);
        assertThat(executionCount()).isEqualTo(1);

        authPost(token, "/api/internal/v1/progression/executions", executionRequest(revision, 31))
                .andExpect(status().isConflict());
        assertThat(globalXp(jogadorId)).isEqualTo(30L);
        assertThat(conhecimentoXp(jogadorId, conhecimentoId)).isEqualTo(30L);
        assertThat(executionCount()).isEqualTo(1);

        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .header("Authorization", "Bearer " + token)
                        .param("sourceSystem", "lifeos")
                        .param("idempotencyKey", IDEMPOTENCY_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.source").value("lifeos"))
                .andExpect(jsonPath("$.identity.idempotencyKey").value(IDEMPOTENCY_KEY))
                .andExpect(jsonPath("$.subject.namespace").value("lifeos"))
                .andExpect(jsonPath("$.subject.externalId").value(EXTERNAL_ID))
                .andExpect(jsonPath("$.configurationKey").value("reading"))
                .andExpect(jsonPath("$.requestedRevision").value(revision));

        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .header("Authorization", "Bearer " + token)
                        .param("subjectNamespace", "lifeos")
                        .param("subjectExternalId", EXTERNAL_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].identity.idempotencyKey").value(IDEMPOTENCY_KEY))
                .andExpect(jsonPath("$.items[0].requestedRevision").value(revision));
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions authPost(String token, String path, String body) throws Exception {
        return mockMvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions action) throws Exception {
        return objectMapper.readTree(action.andReturn().getResponse().getContentAsString());
    }

    private String executionRequest(int revision, int pages) {
        return """
                {"subject":{"namespace":"lifeos","externalId":"%s"},
                "execution":{"source":"lifeos","idempotencyKey":"%s"},
                "configuration":{"key":"reading","revision":%d},
                "details":[{"factorKey":"pages_read","value":%d}]}
                """.formatted(EXTERNAL_ID, IDEMPOTENCY_KEY, revision, pages);
    }

    private long globalXp(UUID jogadorId) {
        return jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId);
    }

    private long conhecimentoXp(UUID jogadorId, UUID conhecimentoId) {
        return jdbc.queryForObject("""
                SELECT aj.xp_total FROM atributo_jogador aj
                JOIN atributo a ON a.id = aj.atributo_id
                WHERE aj.jogador_id = ? AND a.id = ? AND a.nome = 'Conhecimento'
                """, Long.class, jogadorId, conhecimentoId);
    }

    private int executionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM progression_external_execution", Integer.class);
    }
}
