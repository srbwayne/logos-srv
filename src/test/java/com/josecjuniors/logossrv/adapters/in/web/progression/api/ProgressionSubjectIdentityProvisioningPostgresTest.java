package com.josecjuniors.logossrv.adapters.in.web.progression.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.estresseglobal.jpa.EstresseGlobalJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.JpaExternalSubjectResolver;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectLinkChallengeEntity;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectLinkChallengeJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=verified-subject-linking")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProgressionSubjectIdentityProvisioningPostgresTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired EstresseGlobalJpaRepository estresses;
    @Autowired ProgressionSubjectIdentityJpaRepository identities;
    @Autowired ProgressionSubjectLinkChallengeJpaRepository challenges;
    @Autowired JpaExternalSubjectResolver resolver;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @BeforeEach
    void clean() {
        challenges.deleteAll();
        identities.deleteAll();
        estresses.deleteAll();
        jogadores.deleteAll();
        users.deleteAll();
    }

    @Test
    void challengeCreationDoesNotClaimAndIntegrationConfirmationStoresOnlyHash() throws Exception {
        var player = player("link-owner-" + UUID.randomUUID() + "@example.test");
        var challenge = createChallenge(player, "lifeos");
        String rawToken = challenge.get("challengeToken").asText();
        assertThat(rawToken).hasSize(43);
        assertThat(challenge.has("externalId")).isFalse();
        assertThat(challenge.hasNonNull("expiresAt")).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace='lifeos'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM progression_subject_link_challenge", String.class))
                .isEqualTo(hash(rawToken)).isNotEqualTo(rawToken);

        confirm("lifeos", "linked-user", rawToken).andExpect(status().isOk());
        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "linked-user")).value()).isEqualTo(player.userId);
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace='lifeos' AND external_id='linked-user'", String.class))
                .isEqualTo("INTEGRATION_VERIFIED");
        assertThat(jdbc.queryForObject("SELECT verified_by_client_id FROM progression_subject_identity WHERE namespace='lifeos' AND external_id='linked-user'", String.class))
                .isEqualTo("lifeos");
        assertThat(jdbc.queryForObject("SELECT consumed_at IS NOT NULL FROM progression_subject_link_challenge", Boolean.class)).isTrue();
        confirm("lifeos", "linked-user-second", rawToken).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROGRESSION_SUBJECT_LINK_CHALLENGE_INVALID"));
    }

    @Test
    void appUserCannotClaimAndChallengeIsNamespaceOnly() throws Exception {
        var player = player("no-self-claim-" + UUID.randomUUID() + "@example.test");
        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .header("Authorization", "Bearer " + player.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"chosen-by-user\",\"challengeToken\":\"anything\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE external_id='chosen-by-user'", Integer.class)).isZero();

        mockMvc.perform(post("/api/internal/v1/progression/subject-link-challenges")
                        .header("Authorization", "Bearer " + player.token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"namespace\":\"logos-native\"}"))
                .andExpect(status().isBadRequest());
        var challenge = createChallenge(player, "lifeos");
        assertThat(challenge.has("externalId")).isFalse();
    }

    @Test
    void legacyUnverifiedMappingIsUnresolvableAndCanOnlyBePromotedByChallengeOwner() throws Exception {
        var squatter = player("squatter-" + UUID.randomUUID() + "@example.test");
        var rightful = player("rightful-" + UUID.randomUUID() + "@example.test");
        var identity = new ProgressionSubjectIdentity(UUID.randomUUID(), "lifeos", "victim-user", squatter.jogador);
        identities.saveAndFlush(identity);
        assertThat(identity.getVerificationStatus()).isEqualTo("UNVERIFIED");
        assertThatThrownBy(() -> resolver.resolve(new ExternalSubjectReference("lifeos", "victim-user")))
                .isInstanceOf(ProgressionSubjectNotFoundException.class);

        var challenge = createChallenge(rightful, "lifeos");
        confirm("lifeos", "victim-user", challenge.get("challengeToken").asText()).andExpect(status().isOk());
        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "victim-user")).value()).isEqualTo(rightful.userId);
        assertThat(jdbc.queryForObject("SELECT jogador_id FROM progression_subject_identity WHERE namespace='lifeos' AND external_id='victim-user'", UUID.class))
                .isEqualTo(rightful.jogadorId);
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace='lifeos' AND external_id='victim-user'", String.class))
                .isEqualTo("INTEGRATION_VERIFIED");
    }

    @Test
    void verifiedIdentityIsNotReassignedToAnotherPlayer() throws Exception {
        var first = player("verified-first-" + UUID.randomUUID() + "@example.test");
        var second = player("verified-second-" + UUID.randomUUID() + "@example.test");
        var firstChallenge = createChallenge(first, "lifeos");
        confirm("lifeos", "fixed-owner", firstChallenge.get("challengeToken").asText()).andExpect(status().isOk());
        var secondChallenge = createChallenge(second, "lifeos");
        confirm("lifeos", "fixed-owner", secondChallenge.get("challengeToken").asText()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROGRESSION_SUBJECT_IDENTITY_CONFLICT"));
        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "fixed-owner")).value()).isEqualTo(first.userId);

        var sameOwnerRetry = createChallenge(first, "lifeos");
        confirm("lifeos", "fixed-owner", sameOwnerRetry.get("challengeToken").asText()).andExpect(status().isOk());
    }

    @Test
    void invalidExpiredWrongNamespaceAndMixedCredentialsAreRejected() throws Exception {
        var player = player("challenge-errors-" + UUID.randomUUID() + "@example.test");
        confirm("lifeos", "unknown-token", "unknown-random-token").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROGRESSION_SUBJECT_LINK_CHALLENGE_INVALID"));

        var noema = createChallenge(player, "noema");
        confirm("noema", "not-linked", noema.get("challengeToken").asText()).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT consumed_at IS NULL FROM progression_subject_link_challenge", Boolean.class)).isTrue();

        String expiredToken = "expired-challenge-token";
        Instant past = clock.instant().minusSeconds(3600);
        challenges.saveAndFlush(new ProgressionSubjectLinkChallengeEntity(UUID.randomUUID(), hash(expiredToken), "lifeos",
                player.jogadorId, past, past.plusSeconds(60)));
        confirm("lifeos", "expired-user", expiredToken).andExpect(status().isBadRequest());

        var mixed = createChallenge(player, "lifeos");
        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .header("Authorization", "Bearer " + player.token)
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"mixed\",\"challengeToken\":\"" + mixed.get("challengeToken").asText() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentConfirmationsConsumeOneChallengeOnlyOnce() throws Exception {
        var player = player("single-use-race-" + UUID.randomUUID() + "@example.test");
        var challenge = createChallenge(player, "lifeos");
        String token = challenge.get("challengeToken").asText();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var confirmation = (java.util.concurrent.Callable<Integer>) () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start latch timed out");
                return confirm("lifeos", "race-user", token).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(confirmation);
            var second = executor.submit(confirmation);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 400);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_subject_identity WHERE namespace='lifeos' AND external_id='race-user'", Integer.class))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT consumed_at IS NOT NULL FROM progression_subject_link_challenge", Boolean.class)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void registrationKeepsNativeIdentityVerifiedAndIntegrationCredentialsAreRequired() throws Exception {
        var registration = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"native-" + UUID.randomUUID() + "@example.test\",\"password\":\"password\",\"nomeExibicao\":\"Native\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(objectMapper.readTree(registration).get("userId").asText());
        assertThat(jdbc.queryForObject("SELECT verification_status FROM progression_subject_identity WHERE namespace='logos-native' AND external_id=?", String.class, userId.toString()))
                .isEqualTo("LOGOS_NATIVE");
        assertThat(resolver.resolve(new ExternalSubjectReference("logos-native", userId.toString())).value()).isEqualTo(userId);

        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"missing\",\"challengeToken\":\"none\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                        .header("X-Logos-Client-Id", "lifeos")
                        .header("X-Logos-Client-Secret", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespace\":\"lifeos\",\"externalId\":\"missing\",\"challengeToken\":\"none\"}"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode createChallenge(Fixture fixture, String namespace) throws Exception {
        String response = mockMvc.perform(post("/api/internal/v1/progression/subject-link-challenges")
                        .header("Authorization", "Bearer " + fixture.token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"namespace\":\"" + namespace + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String namespace, String externalId, String challenge) throws Exception {
        return mockMvc.perform(post("/api/internal/v1/progression/subject-identities")
                .header("X-Logos-Client-Id", "lifeos")
                .header("X-Logos-Client-Secret", "synthetic-lifeos-integration-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"namespace\":\"" + namespace + "\",\"externalId\":\"" + externalId
                        + "\",\"challengeToken\":\"" + challenge + "\"}"));
    }

    private Fixture player(String email) {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), email, encoder.encode("password")));
        var jogador = new Jogador(JogadorId.generate(), user, "player-" + UUID.randomUUID());
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));
        jogadores.saveAndFlush(jogador);
        return new Fixture(user.getId().getValue(), jogador.getId().getValue(), jwt.generateToken(user), jogador);
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private record Fixture(UUID userId, UUID jogadorId, String token, Jogador jogador) { }
}
