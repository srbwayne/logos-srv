package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionSecurityPostgresTest {
    @Autowired MockMvc mockMvc;
    @Autowired ApplicationContext applicationContext;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void exactReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isForbidden());
    }

    @Test
    void historyReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions/history")
                        .queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "missing"))
                .andExpect(status().isForbidden());
    }

    @Test
    void workloadChainIsAbsentByDefault() {
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
        assertThat(applicationContext.containsBean("progressionExecuteAuthorizationAdvisor")).isFalse();
    }

    @Test
    void malformedHumanBearerRemainsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyHumanBearerRemainsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isForbidden());
    }

    @Test
    void validHumanBearerForUnknownSubjectRemainsUnauthenticated() throws Exception {
        String email = "missing-user-" + UUID.randomUUID() + "@example.test";
        assertThat(appUserCount(email)).isZero();

        String jwt = jwtService.generateToken(User.withUsername(email)
                .password("not-used")
                .authorities(List.of())
                .build());
        assertThat(jwtService.extractUsername(jwt)).isEqualTo(email);

        mockMvc.perform(get("/api/internal/v1/progression/executions")
                        .queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());

        assertThat(appUserCount(email)).isZero();
    }

    private int appUserCount(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM app_user WHERE email = ?", Integer.class, email);
    }
}
