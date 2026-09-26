package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.ProgressionExternalExecutionJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionSecurityPostgresTest {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserJpaRepository users;
    @Autowired ProgressionExternalExecutionJpaRepository executions;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    @Test
    void exactReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historyReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "missing"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidIntegrationCredentialIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "wrong-secret")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sourceAndNamespaceSpoofingAreForbiddenBeforeExecution() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .contentType("application/json")
                        .content("""
                                {"subject":{"namespace":"lifeos","externalId":"subject"},
                                "execution":{"source":"noema","idempotencyKey":"spoof-source"},
                                "configuration":{"key":"missing","revision":1},"details":[]}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .contentType("application/json")
                        .content("""
                                {"subject":{"namespace":"noema","externalId":"subject"},
                                "execution":{"source":"lifeos","idempotencyKey":"spoof-namespace"},
                                "configuration":{"key":"missing","revision":1},"details":[]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void sourceAndHistoryNamespaceSpoofingAreForbidden() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .queryParam("sourceSystem", "noema")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .queryParam("subjectNamespace", "noema")
                        .queryParam("subjectExternalId", "subject"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordinaryAppUserJwtCannotCreateOrReadExecutions() throws Exception {
        String token = jwt.generateToken(users.saveAndFlush(
                new AppUser(new AppUserId(), "ordinary-user@example.test", encoder.encode("password"))));

        mockMvc.perform(post("/api/internal/v1/progression/executions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"subject":{"namespace":"lifeos","externalId":"subject"},
                                "execution":{"source":"lifeos","idempotencyKey":"ordinary-user"},
                                "configuration":{"key":"missing","revision":1},"details":[]}
                                """))
                .andExpect(status().isForbidden());
        assertThat(executions.count()).isZero();

        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "wrong-secret")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "ordinary-user"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "ordinary-user"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "subject"))
                .andExpect(status().isForbidden());
    }
}
