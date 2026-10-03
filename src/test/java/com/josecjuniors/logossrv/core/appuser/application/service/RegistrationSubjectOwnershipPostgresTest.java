package com.josecjuniors.logossrv.core.appuser.application.service;

import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
@TestPropertySource(properties = "logos.test.schema-key=subject-ownership-registration-c1a")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegistrationSubjectOwnershipPostgresTest {

    @Autowired RegistrationUseCase registration;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registrationCreatesNativeClassificationAndInitialHistory() {
        String email = "c1a-native-" + UUID.randomUUID() + "@example.test";
        var result = registration.register(new RegistrationCommand(email, "password", "C1A Native"));
        UUID appUserId = result.user().getId().getValue();
        UUID jogadorId = result.jogador().getId().getValue();

        assertThat(jdbc.queryForMap("""
                SELECT identity.identity_class, identity.ownership_status, identity.verification_status,
                       identity.ownership_version, history.aggregate_version, history.provenance,
                       history.actor_type, history.actor_id, history.evidence_reference,
                       history.effective_at, history.previous_target_jogador_id,
                       history.new_target_jogador_id
                FROM progression_subject_identity identity
                JOIN progression_subject_ownership_history history ON history.identity_id = identity.id
                WHERE identity.namespace = 'logos-native' AND identity.external_id = ?
                """, appUserId.toString()))
                .containsEntry("identity_class", "LOGOS_NATIVE")
                .containsEntry("ownership_status", "ACTIVE")
                .containsEntry("verification_status", "NOT_REQUIRED")
                .containsEntry("ownership_version", 0L)
                .containsEntry("aggregate_version", 0L)
                .containsEntry("provenance", "LOGOS_NATIVE_REGISTRATION")
                .containsEntry("actor_type", null)
                .containsEntry("actor_id", null)
                .containsEntry("evidence_reference", null)
                .containsEntry("previous_target_jogador_id", null)
                .containsEntry("new_target_jogador_id", jogadorId);
        assertThat(jdbc.queryForObject("""
                SELECT effective_at IS NOT NULL FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = 'logos-native' AND identity.external_id = ?
                """, Boolean.class, appUserId.toString())).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_ownership_history history
                JOIN progression_subject_identity identity ON identity.id = history.identity_id
                WHERE identity.namespace = 'logos-native' AND identity.external_id = ?
                """, Integer.class, appUserId.toString())).isEqualTo(1);
    }

    @Test
    void failedInitialHistoryWriteRollsBackRegistrationTransaction() {
        String email = "c1a-rollback-" + UUID.randomUUID() + "@example.test";
        jdbc.execute("""
                CREATE FUNCTION reject_c1a_registration_history()
                RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.provenance = 'LOGOS_NATIVE_REGISTRATION' THEN
                        RAISE EXCEPTION 'intentional C1A registration history failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER trg_reject_c1a_registration_history
                BEFORE INSERT ON progression_subject_ownership_history
                FOR EACH ROW EXECUTE FUNCTION reject_c1a_registration_history()
                """);

        try {
            assertThatThrownBy(() -> registration.register(
                    new RegistrationCommand(email, "password", "C1A Rollback")))
                    .isInstanceOf(RuntimeException.class)
                    .hasStackTraceContaining("intentional C1A registration history failure");
        } finally {
            jdbc.execute("DROP TRIGGER trg_reject_c1a_registration_history ON progression_subject_ownership_history");
            jdbc.execute("DROP FUNCTION reject_c1a_registration_history()");
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE email = ?", Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM jogador j JOIN app_user u ON u.id=j.user_id WHERE u.email = ?",
                Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM progression_subject_identity identity
                JOIN app_user account ON account.id::text = identity.external_id
                WHERE account.email = ? AND identity.namespace = 'logos-native'
                """, Integer.class, email)).isZero();
    }
}
