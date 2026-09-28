package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.adapters.in.web.progression.api.ProgressionExecutionController;
import com.josecjuniors.logossrv.config.jwt.JwtAuthenticationFilter;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.security.workload.admin.application.WorkloadTrustAdministrationService;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActorType;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadReplayStoreUnavailableException;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.AUDIENCE;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.ISSUER;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FreshPostgresIntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WorkloadSpringSecurityIntegrationPostgresTest {
    private static final String EXECUTIONS = "/api/internal/v1/progression/executions";
    private static final String SUBJECT_IDENTITIES = "/api/internal/v1/progression/subject-identities";

    @DynamicPropertySource
    static void enableWorkloadProfileForThisContext(DynamicPropertyRegistry registry) {
        registry.add("logos.security.workload.http.enabled", () -> "true");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired JwtService humanJwtService;
    @Autowired WorkloadTrustAdministrationService trustAdministration;
    @Autowired FilterChainProxy filterChainProxy;
    @Autowired JwtAuthenticationFilter humanJwtFilter;
    @Autowired List<SecurityFilterChain> securityFilterChains;
    @Autowired List<FilterRegistrationBean<?>> filterRegistrations;
    @SpyBean WorkloadAuthenticationService workloadAuthenticationService;
    @SpyBean ProgressionExecutionController progressionController;

    private final List<UUID> replayIds = new ArrayList<>();
    private UUID credentialId;
    private UUID principalDatabaseId;
    private boolean principalCreated;
    private KeyPair keyPair;
    private String kid;

    @BeforeEach
    void setUpTrust() throws Exception {
        reset(workloadAuthenticationService, progressionController);
        principalCreated = jdbc.queryForObject(
                "SELECT count(*) FROM workload_principal WHERE issuer = ?", Integer.class, ISSUER) == 0;
        var actor = new TrustAdministrationActor(TrustAdministrationActorType.SYSTEM, "f1e-r2-test");
        var reason = new TrustAdministrationReason("isolated F1E-R2 integration fixture");
        trustAdministration.registerPrincipal(new WorkloadTrustAdministrationService.RegisterPrincipalCommand(
                new WorkloadPrincipalId(WorkloadAssertionProfile.SUBJECT), new WorkloadIssuer(ISSUER), actor, reason));
        principalDatabaseId = jdbc.queryForObject(
                "SELECT id FROM workload_principal WHERE issuer = ?", UUID.class, ISSUER);

        keyPair = WorkloadTestKeys.ec("secp256r1");
        kid = "f1e-r2-" + UUID.randomUUID();
        var keyId = new WorkloadKeyId(kid);
        trustAdministration.registerCredential(new WorkloadTrustAdministrationService.RegisterCredentialCommand(
                new WorkloadPrincipalId(WorkloadAssertionProfile.SUBJECT), keyId,
                WorkloadTestKeys.publicPem(keyPair), clock.instant().minusSeconds(300), null, actor, reason));
        credentialId = jdbc.queryForObject("SELECT id FROM workload_signing_key WHERE workload_principal_id = ? AND kid = ?",
                UUID.class, principalDatabaseId, kid);
        trustAdministration.changeCredentialLifecycle(new WorkloadTrustAdministrationService.CredentialLifecycleCommand(
                new WorkloadPrincipalId(WorkloadAssertionProfile.SUBJECT), keyId,
                WorkloadCredentialLifecycle.ACTIVE, actor, reason));
    }

    @AfterEach
    void cleanTrustAndThreadLocalState() {
        for (UUID jti : replayIds) {
            jdbc.update("DELETE FROM workload_assertion_replay WHERE issuer = ? AND jti = ?", ISSUER, jti);
        }
        if (credentialId != null) {
            jdbc.update("DELETE FROM workload_trust_audit_event WHERE credential_id = ?", credentialId);
            jdbc.update("DELETE FROM workload_signing_key WHERE id = ?", credentialId);
        }
        if (principalCreated && principalDatabaseId != null) {
            jdbc.update("DELETE FROM workload_trust_audit_event WHERE workload_principal_id = ?", principalDatabaseId);
            jdbc.update("DELETE FROM workload_principal WHERE id = ?", principalDatabaseId);
        }
        reset(workloadAuthenticationService, progressionController);
    }

    @Test
    void workloadChainIsFirstExactAndHumanFilterIsOnlyInHumanChain() {
        assertThat(securityFilterChains).hasSize(2);
        List<SecurityFilterChain> orderedChains = filterChainProxy.getFilterChains();
        assertThat(orderedChains).hasSize(2);
        MockHttpServletRequest workloadPost = request("POST", EXECUTIONS);
        MockHttpServletRequest executionGet = request("GET", EXECUTIONS);
        MockHttpServletRequest historyGet = request("GET", EXECUTIONS + "/history");
        MockHttpServletRequest subjectPost = request("POST", SUBJECT_IDENTITIES);

        assertThat(orderedChains.get(0).matches(workloadPost)).isTrue();
        assertThat(orderedChains.get(0).matches(executionGet)).isFalse();
        assertThat(orderedChains.get(0).matches(historyGet)).isFalse();
        assertThat(orderedChains.get(0).matches(subjectPost)).isFalse();
        assertThat(filtersFor(workloadPost))
                .anyMatch(WorkloadBearerAuthenticationFilter.class::isInstance)
                .noneMatch(JwtAuthenticationFilter.class::isInstance);
        assertThat(filtersFor(executionGet)).contains(humanJwtFilter);
        assertThat(filtersFor(historyGet)).contains(humanJwtFilter);
        assertThat(filtersFor(subjectPost)).contains(humanJwtFilter);

        FilterRegistrationBean<?> registration = filterRegistrations.stream()
                .filter(bean -> bean.getFilter() == humanJwtFilter).findFirst().orElseThrow();
        assertThat(registration.isEnabled()).isFalse();
    }

    @Test
    void validAssertionIsConsumedBeforeDenyAllAndReplayIsUnauthorized() throws Exception {
        UUID jti = UUID.randomUUID();
        replayIds.add(jti);
        String assertion = workloadAssertion(keyPair, kid, jti);

        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer " + assertion)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertReplayCount(jti, 1);
        verify(progressionController, never()).create(any());

        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer " + assertion)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertReplayCount(jti, 1);
        verify(progressionController, never()).create(any());
    }

    @Test
    void workloadFailuresAreUnauthorizedAndHumanJwtDoesNotFallback() throws Exception {
        mockMvc.perform(post(EXECUTIONS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());

        String humanJwt = humanJwtService.generateToken(User.withUsername("human@example.test")
                .password("not-used").authorities(List.of()).build());
        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer " + humanJwt))
                .andExpect(status().isUnauthorized());

        KeyPair wrongKey = WorkloadTestKeys.ec("secp256r1");
        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer "
                        + workloadAssertion(wrongKey, kid, UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
        verify(progressionController, never()).create(any());
    }

    @Test
    void authenticationInfrastructureFailuresReturnServiceUnavailable() throws Exception {
        doThrow(new WorkloadReplayStoreUnavailableException()).when(workloadAuthenticationService)
                .authenticate(anyString());
        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer infrastructure-test"))
                .andExpect(status().isServiceUnavailable());
        reset(workloadAuthenticationService);

        doThrow(new WorkloadTrustRegistryIntegrityException("test integrity failure"))
                .when(workloadAuthenticationService).authenticate(anyString());
        mockMvc.perform(post(EXECUTIONS).header("Authorization", "Bearer integrity-test"))
                .andExpect(status().isServiceUnavailable());
        verify(progressionController, never()).create(any());
    }

    @Test
    void nonWorkloadRoutesRemainOnHumanChainWhenProfileIsEnabled() throws Exception {
        mockMvc.perform(get(EXECUTIONS).queryParam("sourceSystem", "lifeos")
                        .queryParam("idempotencyKey", "missing"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(EXECUTIONS + "/history").queryParam("subjectNamespace", "lifeos")
                        .queryParam("subjectExternalId", "missing"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(SUBJECT_IDENTITIES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    private void assertReplayCount(UUID jti, long expected) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_assertion_replay WHERE issuer = ? AND jti = ?",
                Long.class, ISSUER, jti)).isEqualTo(expected);
    }

    private List<jakarta.servlet.Filter> filtersFor(MockHttpServletRequest request) {
        SecurityFilterChain matchingChain = filterChainProxy.getFilterChains().stream()
                .filter(chain -> chain.matches(request)).findFirst().orElseThrow();
        return ((DefaultSecurityFilterChain) matchingChain).getFilters();
    }

    private String workloadAssertion(KeyPair signer, String assertionKid, UUID jti) {
        Instant now = clock.instant();
        return Jwts.builder().header().type(TYPE).keyId(assertionKid).and()
                .issuer(ISSUER).subject(WorkloadAssertionProfile.SUBJECT).audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now.minusSeconds(1))).expiration(Date.from(now.plusSeconds(59)))
                .id(jti.toString()).signWith(signer.getPrivate(), Jwts.SIG.ES256).compact();
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
