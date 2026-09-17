package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionExecutionSecurityPostgresTest {
    @Autowired MockMvc mockMvc;

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
}
