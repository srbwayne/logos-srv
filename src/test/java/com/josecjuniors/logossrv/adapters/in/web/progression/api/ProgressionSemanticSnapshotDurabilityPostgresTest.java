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
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=semantic-snapshot-durability")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionSemanticSnapshotDurabilityPostgresTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void freezesSemanticIdentityAcrossReplayExactReadAndHistory() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "snapshot-" + suffix + "@example.test";
        String password = "snapshot-password";
        String externalId = "snapshot-user-" + suffix;
        String beforeKey = "semantic-snapshot-before-" + suffix;
        String afterKey = "semantic-snapshot-after-" + suffix;

        JsonNode registration = json(mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"nomeExibicao\":\"Snapshot POC\"}".formatted(email, password)))
                .andExpect(status().isOk()));
        UUID jogadorId = UUID.fromString(registration.get("jogadorId").asText());
        String token = json(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())).get("token").asText();

        JsonNode attribute = json(authPost(token, "/api/atributos", "{\"nome\":\"Conhecimento\",\"descricao\":\"snapshot\"}")
                .andExpect(status().isCreated()));
        UUID attributeId = UUID.fromString(attribute.get("id").asText());
        json(authPost(token, "/api/fatores-calculo", """
                {"semanticKey":"pages_read","nome":"Pages Read","unidadeMedida":"pages","tipoInput":"NUMERICO"}
                """).andExpect(status().isCreated()));
        JsonNode definition = json(authPost(token, "/api/progression/configurations", "{\"logicalKey\":\"reading-snapshot-%s\"}".formatted(suffix))
                .andExpect(status().isCreated()));
        String logicalKey = definition.get("logicalKey").asText();
        long draftVersion = definition.get("draftVersion").asLong();
        long activationVersion = definition.get("activationVersion").asLong();
        JsonNode draft = json(authPut(token, "/api/progression/configurations/%s/draft".formatted(logicalKey), """
                {"expectedVersion":%d,"baseXp":1,"baseStress":0,"factors":["pages_read"],
                "distributions":[{"attributeId":"%s","weight":1.0,"xpRules":[{"fact":"pages_read","multiplier":1.0,"minCutoff":null,"maxCutoff":null,"calculationMode":"FACT_VALUE"}],"stressRules":[]}]}
                """.formatted(draftVersion, attributeId)).andExpect(status().isOk()));
        int revision = json(authPost(token, "/api/progression/configurations/%s/publish".formatted(logicalKey), "{\"expectedDraftVersion\":%d}".formatted(draft.get("version").asLong()))
                .andExpect(status().isOk())).get("revision").asInt();
        json(authPost(token, "/api/progression/configurations/%s/versions/%d/activate".formatted(logicalKey, revision),
                "{\"expectedActivationVersion\":%d}".formatted(activationVersion)).andExpect(status().isOk()));
        json(authPost(token, "/api/internal/v1/progression/subject-identities", "{\"namespace\":\"lifeos\",\"externalId\":\"%s\"}".formatted(externalId))
                .andExpect(status().isOk()));

        String beforeRequest = execution(externalId, beforeKey, logicalKey, revision, 3);
        authPost(token, "/api/internal/v1/progression/executions", beforeRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.attributeProgressions[0].key").value(attributeId.toString()))
                .andExpect(jsonPath("$.result.attributeProgressions[0].semanticKey").value((Object) null))
                .andExpect(jsonPath("$.profile.attributes[0].semanticKey").value((Object) null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_external_execution", Integer.class)).isEqualTo(1);
        exact(token, beforeKey, null);
        history(token, externalId, beforeKey, null);

        authPut(token, "/api/atributos/%s/semantic-key".formatted(attributeId), "{\"semanticKey\":\"knowledge\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.semanticKey").value("knowledge"));

        authPost(token, "/api/internal/v1/progression/executions", beforeRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.attributeProgressions[0].semanticKey").value((Object) null))
                .andExpect(jsonPath("$.profile.attributes[0].semanticKey").value((Object) null));
        exact(token, beforeKey, null);
        history(token, externalId, beforeKey, null);

        String afterRequest = execution(externalId, afterKey, logicalKey, revision, 3);
        authPost(token, "/api/internal/v1/progression/executions", afterRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.attributeProgressions[0].semanticKey").value("knowledge"))
                .andExpect(jsonPath("$.profile.attributes[0].semanticKey").value("knowledge"));
        exact(token, afterKey, "knowledge");
        history(token, externalId, beforeKey, null);
        history(token, externalId, afterKey, "knowledge");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM progression_external_execution", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT xp_total FROM jogador WHERE id = ?", Long.class, jogadorId)).isEqualTo(6L);
    }

    private org.springframework.test.web.servlet.ResultActions authPost(String token, String path, String body) throws Exception {
        return mockMvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private org.springframework.test.web.servlet.ResultActions authPut(String token, String path, String body) throws Exception {
        return mockMvc.perform(put(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private JsonNode json(org.springframework.test.web.servlet.ResultActions action) throws Exception {
        return objectMapper.readTree(action.andReturn().getResponse().getContentAsString());
    }
    private String execution(String subject, String idempotency, String key, int revision, int pages) {
        return "{\"subject\":{\"namespace\":\"lifeos\",\"externalId\":\"%s\"},\"execution\":{\"source\":\"lifeos\",\"idempotencyKey\":\"%s\"},\"configuration\":{\"key\":\"%s\",\"revision\":%d},\"details\":[{\"factorKey\":\"pages_read\",\"value\":%d}]}"
                .formatted(subject, idempotency, key, revision, pages);
    }
    private void exact(String token, String idempotency, String expected) throws Exception {
        var result = mockMvc.perform(get("/api/internal/v1/progression/executions").header("Authorization", "Bearer " + token)
                        .param("sourceSystem", "lifeos").param("idempotencyKey", idempotency))
                .andExpect(status().isOk());
        if (expected == null) result.andExpect(jsonPath("$.outcome.result.attributeProgressions[0].semanticKey").value((Object) null));
        else result.andExpect(jsonPath("$.outcome.result.attributeProgressions[0].semanticKey").value(expected));
    }
    private void history(String token, String subject, String idempotency, String expected) throws Exception {
        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .header("Authorization", "Bearer " + token).param("subjectNamespace", "lifeos")
                        .param("subjectExternalId", subject).param("page", "0").param("size", "20"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode item = null;
        for (JsonNode candidate : body.get("items")) if (idempotency.equals(candidate.get("identity").get("idempotencyKey").asText())) item = candidate;
        assertThat(item).isNotNull();
        JsonNode semantic = item.at("/outcome/result/attributeProgressions/0/semanticKey");
        if (expected == null) assertThat(semantic.isNull()).isTrue(); else assertThat(semantic.asText()).isEqualTo(expected);
    }
}
