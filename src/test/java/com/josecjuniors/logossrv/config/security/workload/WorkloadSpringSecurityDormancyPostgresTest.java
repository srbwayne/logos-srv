package com.josecjuniors.logossrv.config.security.workload;

import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import com.josecjuniors.logossrv.support.test.WorkloadTestKeys;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.AUDIENCE;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.ISSUER;
import static com.josecjuniors.logossrv.core.security.workload.application.WorkloadAssertionProfile.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
class WorkloadSpringSecurityDormancyPostgresTest {
    @Autowired ApplicationContext applicationContext;
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkloadAuthenticationService workloadAuthenticationService;
    @Autowired JwtService humanJwtService;
    @Autowired Clock clock;

    private UUID principalDbId;

    @AfterEach
    void cleanup() {
        if (principalDbId != null) {
            jdbc.update("DELETE FROM workload_assertion_replay WHERE issuer = ?", ISSUER);
            jdbc.update("DELETE FROM workload_trust_audit_event WHERE workload_principal_id = ?", principalDbId);
            jdbc.update("DELETE FROM workload_signing_key WHERE workload_principal_id = ?", principalDbId);
            jdbc.update("DELETE FROM workload_principal WHERE id = ?", principalDbId);
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void workloadAdapterIsNotAutomaticallyRegisteredInProductionContext() {
        assertThat(applicationContext.getBeansOfType(WorkloadBearerAuthenticationFilter.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(WorkloadAuthenticationProvider.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
    }

    @Test
    void realWorkloadAssertionAuthenticatesThroughAdapterButNotHumanJwtProfile() throws Exception {
        KeyPair pair = provisionKey("kid-profile-isolation");
        WorkloadBearerAuthenticationFilter filter = filter();
        String workloadAssertion = workloadAssertion(pair, UUID.randomUUID());
        Authentication[] downstream = new Authentication[1];

        var firstResponse = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request("Bearer " + workloadAssertion), firstResponse,
                (req, res) -> downstream[0] = SecurityContextHolder.getContext().getAuthentication());
        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(downstream[0]).isInstanceOf(WorkloadPrincipalAuthenticationToken.class);
        assertThat(((WorkloadPrincipalAuthenticationToken) downstream[0]).getPrincipal())
                .isInstanceOf(AuthenticatedPrincipal.class);

        var replayResponse = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request("Bearer " + workloadAssertion), replayResponse,
                (req, res) -> { throw new AssertionError("replayed workload assertion must not continue"); });
        assertThat(replayResponse.getStatus()).isEqualTo(401);

        SecretKey humanKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String humanStyleJwt = Jwts.builder().subject("person@example.test")
                .signWith(humanKey, Jwts.SIG.HS256).compact();
        var humanBearerResponse = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request("Bearer " + humanStyleJwt), humanBearerResponse,
                (req, res) -> { throw new AssertionError("human JWT must not pass workload authentication"); });
        assertThat(humanBearerResponse.getStatus()).isEqualTo(401);

        assertThatThrownBy(() -> humanJwtService.extractUsername(workloadAssertion))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workload_assertion_replay WHERE issuer = ?",
                Long.class, ISSUER)).isEqualTo(1);
    }

    private WorkloadBearerAuthenticationFilter filter() {
        return new WorkloadBearerAuthenticationFilter(
                new ProviderManager(List.of(new WorkloadAuthenticationProvider(workloadAuthenticationService))),
                request -> request.getRequestURI().startsWith("/__test/workload/"),
                new WorkloadAuthenticationFailureResponder());
    }

    private KeyPair provisionKey(String kid) throws Exception {
        KeyPair pair = WorkloadTestKeys.ec("secp256r1");
        principalDbId = UUID.randomUUID();
        UUID credentialId = UUID.randomUUID();
        Instant notBefore = clock.instant().minusSeconds(300);
        jdbc.update("""
                INSERT INTO workload_principal (id, principal_type, principal_id, issuer, lifecycle_status)
                VALUES (?, 'WORKLOAD', 'lifeos', ?, 'ACTIVE')
                """, principalDbId, ISSUER);
        jdbc.update("""
                INSERT INTO workload_signing_key
                    (id, workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                     lifecycle_status, not_before, not_after, activated_at)
                VALUES (?, ?, ?, 'ES256', ?, ?, 'ACTIVE', ?, NULL, ?)
                """, credentialId, principalDbId, kid, WorkloadTestKeys.publicPem(pair),
                MessageDigest.getInstance("SHA-256").digest(pair.getPublic().getEncoded()),
                java.sql.Timestamp.from(notBefore), java.sql.Timestamp.from(notBefore));
        this.kid = kid;
        return pair;
    }

    private String kid;

    private String workloadAssertion(KeyPair pair, UUID jti) {
        Instant now = clock.instant();
        return Jwts.builder().header().type(TYPE).keyId(kid).and()
                .issuer(ISSUER).subject("lifeos").audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now.minusSeconds(1))).expiration(Date.from(now.plusSeconds(59)))
                .id(jti.toString()).signWith(pair.getPrivate(), Jwts.SIG.ES256).compact();
    }

    private static org.springframework.mock.web.MockHttpServletRequest request(String authorization) {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/__test/workload/isolation");
        request.addHeader("Authorization", authorization);
        return request;
    }
}
