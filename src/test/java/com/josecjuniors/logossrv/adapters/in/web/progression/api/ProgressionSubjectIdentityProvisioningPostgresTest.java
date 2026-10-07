package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.estresseglobal.jpa.EstresseGlobalJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.JpaExternalSubjectResolver;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-identity-provisioning-c1a")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionSubjectIdentityProvisioningPostgresTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired EstresseGlobalJpaRepository estresses;
    @Autowired JpaExternalSubjectResolver resolver;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProgressionSubjectIdentityProvisioningPort identityProvisioning;

    @Test
    void registrationCreatesAndResolverReadsLogosNativeMapping() throws Exception {
        var response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"native-" + UUID.randomUUID() + "@example.com\",\"password\":\"password\",\"nomeExibicao\":\"Native\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        UUID userId = UUID.fromString(body.get("userId").asText());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace = 'logos-native' AND external_id = ?", Integer.class, userId.toString()))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT identity.identity_class, identity.ownership_status, identity.verification_status,
                       identity.ownership_version, history.provenance, history.actor_type,
                       history.actor_id, history.evidence_reference
                FROM progression_subject_identity identity
                JOIN progression_subject_ownership_history history ON history.identity_id = identity.id
                WHERE identity.namespace = 'logos-native' AND identity.external_id = ?
                """, userId.toString()))
                .containsEntry("identity_class", "LOGOS_NATIVE")
                .containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "NOT_REQUIRED")
                .containsEntry("ownership_version", 0L)
                .containsEntry("provenance", "LOGOS_NATIVE_REGISTRATION")
                .containsEntry("actor_type", null)
                .containsEntry("actor_id", null)
                .containsEntry("evidence_reference", null);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = 'logos-native' AND identity.external_id = ?
                """, Integer.class, userId.toString())).isEqualTo(1);
        assertThat(resolver.resolve(new ExternalSubjectReference(" LOGOS-NATIVE ", userId.toString())).value())
                .isEqualTo(userId);
    }

    @Test
    void selfProvisionNormalizesAndResolvesCurrentPlayer() throws Exception {
        var fixture = player("self-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .principal(() -> fixture.email)
                        .header("Authorization", "Bearer " + fixture.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\" LIFEOS \",\"externalId\":\" user-123 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.namespace").value("lifeos"))
                .andExpect(jsonPath("$.externalId").value("user-123"));

        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "user-123")).value())
                .isEqualTo(fixture.userId);
        assertPocClassification(fixture.userId, "lifeos", "user-123");
    }

    @Test
    void repeatedSelfProvisionIsIdempotent() throws Exception {
        var fixture = player("repeat-" + UUID.randomUUID() + "@example.com");
        var request = "{\"namespace\":\"lifeos\",\"externalId\":\"repeat\"}";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                            .header("Authorization", "Bearer " + fixture.token)
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isOk());
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace = 'lifeos' AND external_id = 'repeat'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = 'lifeos' AND identity.external_id = 'repeat'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentFirstProvisionClaimsOnePointerAndIdentity() throws Exception {
        var fixture = player("concurrent-first-" + UUID.randomUUID() + "@example.com");
        var reference = new ExternalSubjectReference("lifeos", "first-" + UUID.randomUUID());
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                provisionConcurrently(reference, fixture.userId, ready, start);
                return null;
            });
            var second = executor.submit(() -> {
                provisionConcurrently(reference, fixture.userId, ready, start);
                return null;
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_identity
                WHERE namespace = ? AND external_id = ?
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_current_binding
                WHERE namespace = ? AND external_id = ?
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, Integer.class, reference.namespace(), reference.externalId())).isEqualTo(1);
    }

    private void provisionConcurrently(ExternalSubjectReference reference, UUID targetId,
                                      CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent provision did not start");
        identityProvisioning.provision(reference, new SubjectId(targetId));
    }

    @Test
    void conflictingOwnerReturns409AndDoesNotReassign() throws Exception {
        var first = player("first-" + UUID.randomUUID() + "@example.com");
        var second = player("second-" + UUID.randomUUID() + "@example.com");
        var request = "{\"namespace\":\"lifeos\",\"externalId\":\"shared\"}";

        provision(first, request).andExpect(status().isOk());
        provision(second, request).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROGRESSION_SUBJECT_IDENTITY_CONFLICT"));

        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "shared")).value())
                .isEqualTo(first.userId);
        assertPocClassification(first.userId, "lifeos", "shared");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = 'lifeos' AND identity.external_id = 'shared'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void samePlayerMayOwnMultipleExternalIdentities() throws Exception {
        var fixture = player("multiple-" + UUID.randomUUID() + "@example.com");
        provision(fixture, "{\"namespace\":\"lifeos\",\"externalId\":\"A\"}").andExpect(status().isOk());
        provision(fixture, "{\"namespace\":\"noema\",\"externalId\":\"B\"}").andExpect(status().isOk());

        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "A")).value()).isEqualTo(fixture.userId);
        assertThat(resolver.resolve(new ExternalSubjectReference("noema", "B")).value()).isEqualTo(fixture.userId);
    }

    @Test
    void logosNativeCannotBeSelfProvisioned() throws Exception {
        var fixture = player("reserved-" + UUID.randomUUID() + "@example.com");

        provision(fixture, "{\"namespace\":\" LOGOS-NATIVE \",\"externalId\":\"fake\"}")
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace = 'logos-native' AND external_id = 'fake'", Integer.class))
                .isZero();
    }

    @Test
    void unauthenticatedProvisioningIsForbidden() throws Exception {
        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"anonymous\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserWithoutPlayerReturns404() throws Exception {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), "orphan-" + UUID.randomUUID() + "@example.com", encoder.encode("password")));

        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .header("Authorization", "Bearer " + jwt.generateToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"orphan\"}"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions provision(Fixture fixture, String request) throws Exception {
        return mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                .header("Authorization", "Bearer " + fixture.token)
                .contentType(MediaType.APPLICATION_JSON).content(request));
    }

    private void assertPocClassification(UUID actorId, String namespace, String externalId) {
        assertThat(jdbc.queryForMap("""
                SELECT identity.identity_class, identity.ownership_status, identity.verification_status,
                       identity.ownership_version, history.aggregate_version, history.provenance,
                       history.actor_type, history.actor_id, history.evidence_reference,
                       history.effective_at, history.previous_target_jogador_id,
                       history.new_target_jogador_id
                FROM progression_subject_identity identity
                JOIN progression_subject_ownership_history history ON history.identity_id = identity.id
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, namespace, externalId))
                .containsEntry("identity_class", "EXTERNAL")
                .containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "UNVERIFIED")
                .containsEntry("ownership_version", 0L)
                .containsEntry("aggregate_version", 0L)
                .containsEntry("provenance", "POC_SELF_LINK")
                .containsEntry("actor_type", "APP_USER")
                .containsEntry("actor_id", actorId.toString())
                .containsEntry("evidence_reference", null)
                .containsEntry("previous_target_jogador_id", null);
        assertThat(jdbc.queryForObject("""
                SELECT history.effective_at IS NOT NULL FROM progression_subject_identity identity
                JOIN progression_subject_ownership_history history ON history.identity_id = identity.id
                WHERE identity.namespace = ? AND identity.external_id = ?
                """, Boolean.class, namespace, externalId)).isTrue();
    }

    private Fixture player(String email) {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), email, encoder.encode("password")));
        var player = new Jogador(JogadorId.generate(), user, "player-" + UUID.randomUUID());
        player.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), player));
        jogadores.saveAndFlush(player);
        return new Fixture(user.getId().getValue(), email, jwt.generateToken(user));
    }

    private record Fixture(UUID userId, String email, String token) { }
}
