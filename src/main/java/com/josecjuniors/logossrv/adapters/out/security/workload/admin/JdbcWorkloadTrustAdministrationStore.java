package com.josecjuniors.logossrv.adapters.out.security.workload.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josecjuniors.logossrv.adapters.out.security.workload.crypto.SpkiPemWorkloadPublicKeyParser;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.InvalidWorkloadPublicKeyException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationError;
import com.josecjuniors.logossrv.core.security.workload.admin.application.exception.TrustAdministrationException;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.CredentialSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.PrincipalSnapshot;
import com.josecjuniors.logossrv.core.security.workload.admin.application.port.out.WorkloadTrustAdministrationStore.RegistrationResult;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationActor;
import com.josecjuniors.logossrv.core.security.workload.admin.domain.TrustAdministrationReason;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcWorkloadTrustAdministrationStore implements WorkloadTrustAdministrationStore {
    private static final String INSERT_PRINCIPAL = """
            INSERT INTO workload_principal
                (principal_type, principal_id, issuer, lifecycle_status, created_at, updated_at)
            VALUES ('WORKLOAD', ?, ?, 'ACTIVE', ?, ?)
            ON CONFLICT DO NOTHING
            RETURNING id
            """;
    private static final String INSERT_CREDENTIAL = """
            INSERT INTO workload_signing_key
                (workload_principal_id, kid, algorithm, public_key_pem, public_key_spki_sha256,
                 lifecycle_status, not_before, not_after, created_at, updated_at)
            VALUES (?, ?, 'ES256', ?, ?, 'PENDING', ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            RETURNING id
            """;
    private static final String INSERT_AUDIT = """
            INSERT INTO workload_trust_audit_event
                (workload_principal_id, credential_id, actor_type, actor_id, action, reason, occurred_at, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """;
    private static final List<String> ACTIONS = List.of("WORKLOAD_REGISTERED", "WORKLOAD_DISABLED",
            "WORKLOAD_ENABLED", "WORKLOAD_REVOKED", "CREDENTIAL_REGISTERED", "CREDENTIAL_ACTIVATED",
            "CREDENTIAL_REVOKED", "CREDENTIAL_RETIRED");

    private final JdbcTemplate jdbc;
    private final SpkiPemWorkloadPublicKeyParser publicKeyParser;
    private final ObjectMapper objectMapper;

    public JdbcWorkloadTrustAdministrationStore(JdbcTemplate jdbc,
                                                SpkiPemWorkloadPublicKeyParser publicKeyParser,
                                                ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.publicKeyParser = publicKeyParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public RegistrationResult registerPrincipal(WorkloadPrincipalId principalId, WorkloadIssuer issuer,
                                                TrustAdministrationActor actor, TrustAdministrationReason reason,
                                                Instant now) {
        try {
            List<UUID> inserted = jdbc.query(INSERT_PRINCIPAL, (rs, row) -> UUID.fromString(rs.getString("id")),
                    principalId.value(), issuer.value(), timestamp(now), timestamp(now));
            if (!inserted.isEmpty()) {
                appendAudit(inserted.get(0), null, actor, reason, "WORKLOAD_REGISTERED",
                        Map.of("principalType", "WORKLOAD", "principalId", principalId.value(),
                                "issuer", issuer.value(), "oldLifecycle", "ABSENT", "newLifecycle", "ACTIVE"), now);
                return RegistrationResult.CREATED;
            }

            List<PrincipalIdentityRow> byId = jdbc.query("""
                    SELECT id, principal_id, issuer FROM workload_principal
                    WHERE principal_type = 'WORKLOAD' AND principal_id = ?
                    """, (rs, row) -> new PrincipalIdentityRow(UUID.fromString(rs.getString("id")),
                    rs.getString("principal_id"), rs.getString("issuer")), principalId.value());
            if (!byId.isEmpty()) {
                if (issuer.value().equals(byId.get(0).issuer())) return RegistrationResult.NO_OP;
                throw failure(TrustAdministrationError.PRINCIPAL_IDENTITY_CONFLICT);
            }
            List<String> ownerByIssuer = jdbc.query("SELECT principal_id FROM workload_principal WHERE issuer = ?",
                    (rs, row) -> rs.getString(1), issuer.value());
            if (!ownerByIssuer.isEmpty()) throw failure(TrustAdministrationError.ISSUER_CONFLICT);
            throw failure(TrustAdministrationError.INTEGRITY_FAILURE);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw persistence(exception);
        }
    }

    @Override
    public RegistrationResult registerCredential(WorkloadPrincipalId principalId, WorkloadKeyId kid,
                                                 String publicKeyPem, Instant notBefore, Instant notAfter,
                                                 TrustAdministrationActor actor, TrustAdministrationReason reason,
                                                 Instant now) {
        try {
            PrincipalSnapshot principal = lockPrincipal(principalId);
            var publicKey = parseProspectiveKey(publicKeyPem);
            byte[] fingerprint = sha256(publicKey.getEncoded());

            List<ExistingCredentialRow> existingByKid = jdbc.query("""
                    SELECT id, algorithm, public_key_spki_sha256, not_before, not_after, lifecycle_status
                    FROM workload_signing_key
                    WHERE workload_principal_id = ? AND kid = ?
                    """, (rs, row) -> new ExistingCredentialRow(UUID.fromString(rs.getString("id")),
                    rs.getString("algorithm"), rs.getBytes("public_key_spki_sha256"),
                    instant(rs.getTimestamp("not_before")), instant(rs.getTimestamp("not_after")),
                    rs.getString("lifecycle_status")), principal.id(), kid.value());
            if (!existingByKid.isEmpty()) {
                ExistingCredentialRow existing = existingByKid.get(0);
                boolean identical = "ES256".equals(existing.algorithm())
                        && Arrays.equals(fingerprint, existing.fingerprint())
                        && notBefore.equals(existing.notBefore())
                        && java.util.Objects.equals(notAfter, existing.notAfter());
                if (identical) return RegistrationResult.NO_OP;
                throw failure(TrustAdministrationError.KID_CONFLICT);
            }
            if (principal.lifecycle() == WorkloadPrincipalLifecycle.REVOKED) {
                throw failure(TrustAdministrationError.PRINCIPAL_REVOKED);
            }

            List<UUID> inserted = jdbc.query(INSERT_CREDENTIAL, (rs, row) -> UUID.fromString(rs.getString("id")),
                    principal.id(), kid.value(), publicKeyPem, fingerprint, timestamp(notBefore),
                    notAfter == null ? null : timestamp(notAfter), timestamp(now), timestamp(now));
            if (!inserted.isEmpty()) {
                String fingerprintHex = HexFormat.of().formatHex(fingerprint);
                appendAudit(principal.id(), inserted.get(0), actor, reason, "CREDENTIAL_REGISTERED",
                        Map.of("principalType", "WORKLOAD", "principalId", principal.principalId(),
                                "issuer", principal.issuer(), "kid", kid.value(), "algorithm", "ES256",
                                "fingerprint", fingerprintHex, "notBefore", notBefore.toString(),
                                "newLifecycle", "PENDING", "notAfter", notAfter == null ? "" : notAfter.toString()), now);
                return RegistrationResult.CREATED;
            }

            List<UUID> ownerByFingerprint = jdbc.query(
                    "SELECT id FROM workload_signing_key WHERE public_key_spki_sha256 = ?",
                    (rs, row) -> UUID.fromString(rs.getString(1)), fingerprint);
            if (!ownerByFingerprint.isEmpty()) throw failure(TrustAdministrationError.FINGERPRINT_CONFLICT);
            throw failure(TrustAdministrationError.INTEGRITY_FAILURE);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw persistence(exception);
        }
    }

    @Override
    public PrincipalSnapshot lockPrincipal(WorkloadPrincipalId principalId) {
        try {
            List<PrincipalSnapshot> rows = jdbc.query("""
                    SELECT id, principal_id, issuer, lifecycle_status, disabled_at, revoked_at
                    FROM workload_principal
                    WHERE principal_type = 'WORKLOAD' AND principal_id = ?
                    FOR UPDATE
                    """, (rs, row) -> new PrincipalSnapshot(UUID.fromString(rs.getString("id")),
                    rs.getString("principal_id"), rs.getString("issuer"),
                    principalLifecycle(rs.getString("lifecycle_status")), instant(rs.getTimestamp("disabled_at")),
                    instant(rs.getTimestamp("revoked_at"))), principalId.value());
            if (rows.isEmpty()) throw failure(TrustAdministrationError.PRINCIPAL_NOT_FOUND);
            return rows.get(0);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw persistence(exception);
        }
    }

    @Override
    public CredentialSnapshot lockCredential(UUID principalDatabaseId, WorkloadKeyId kid) {
        try {
            List<CredentialSnapshot> rows = jdbc.query("""
                    SELECT id, workload_principal_id, kid, lifecycle_status, activated_at, revoked_at, retired_at
                    FROM workload_signing_key
                    WHERE workload_principal_id = ? AND kid = ?
                    FOR UPDATE
                    """, (rs, row) -> new CredentialSnapshot(UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("workload_principal_id")), rs.getString("kid"),
                    credentialLifecycle(rs.getString("lifecycle_status")), instant(rs.getTimestamp("activated_at")),
                    instant(rs.getTimestamp("revoked_at")), instant(rs.getTimestamp("retired_at"))),
                    principalDatabaseId, kid.value());
            if (rows.isEmpty()) throw failure(TrustAdministrationError.CREDENTIAL_NOT_FOUND);
            return rows.get(0);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw persistence(exception);
        }
    }

    @Override
    public void updatePrincipalLifecycle(PrincipalSnapshot principal, WorkloadPrincipalLifecycle target, Instant now) {
        String sql = switch (target) {
            case ACTIVE -> "UPDATE workload_principal SET lifecycle_status='ACTIVE', disabled_at=NULL, revoked_at=NULL, updated_at=? WHERE id=? AND lifecycle_status=?";
            case DISABLED -> "UPDATE workload_principal SET lifecycle_status='DISABLED', disabled_at=?, updated_at=? WHERE id=? AND lifecycle_status=?";
            case REVOKED -> "UPDATE workload_principal SET lifecycle_status='REVOKED', revoked_at=?, updated_at=? WHERE id=? AND lifecycle_status=?";
        };
        Object[] parameters = switch (target) {
            case ACTIVE -> new Object[]{timestamp(now), principal.id(), principal.lifecycle().name()};
            case DISABLED -> new Object[]{timestamp(now), timestamp(now), principal.id(), principal.lifecycle().name()};
            case REVOKED -> new Object[]{timestamp(now), timestamp(now), principal.id(), principal.lifecycle().name()};
        };
        executeExpectedOne(sql, parameters);
    }

    @Override
    public void updateCredentialLifecycle(CredentialSnapshot credential, WorkloadCredentialLifecycle target, Instant now) {
        String sql = switch (target) {
            case ACTIVE -> "UPDATE workload_signing_key SET lifecycle_status='ACTIVE', activated_at=?, revoked_at=NULL, retired_at=NULL, updated_at=? WHERE id=? AND lifecycle_status=?";
            case REVOKED -> "UPDATE workload_signing_key SET lifecycle_status='REVOKED', revoked_at=?, retired_at=NULL, updated_at=? WHERE id=? AND lifecycle_status=?";
            case RETIRED -> "UPDATE workload_signing_key SET lifecycle_status='RETIRED', retired_at=?, updated_at=? WHERE id=? AND lifecycle_status=?";
            case PENDING -> throw failure(TrustAdministrationError.INVALID_LIFECYCLE_TRANSITION);
        };
        executeExpectedOne(sql, timestamp(now), timestamp(now), credential.id(), credential.lifecycle().name());
    }

    @Override
    public void appendAudit(UUID principalDatabaseId, UUID credentialDatabaseId, TrustAdministrationActor actor,
                            TrustAdministrationReason reason, String action, Map<String, Object> metadata, Instant now) {
        if (!ACTIONS.contains(action)) throw failure(TrustAdministrationError.INTEGRITY_FAILURE);
        try {
            String json = objectMapper.writeValueAsString(metadata);
            jdbc.update(INSERT_AUDIT, principalDatabaseId, credentialDatabaseId, actor.actorType().name(),
                    actor.actorId(), action, reason.value(), timestamp(now), json);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException | JsonProcessingException exception) {
            throw persistence(exception);
        }
    }

    private void executeExpectedOne(String sql, Object... parameters) {
        try {
            int changed = jdbc.update(sql, parameters);
            if (changed != 1) throw failure(TrustAdministrationError.INTEGRITY_FAILURE);
        } catch (TrustAdministrationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw persistence(exception);
        }
    }

    private java.security.interfaces.ECPublicKey parseProspectiveKey(String pem) {
        try {
            return publicKeyParser.parseP256(pem);
        } catch (WorkloadTrustRegistryIntegrityException exception) {
            throw new InvalidWorkloadPublicKeyException(exception);
        }
    }

    private static byte[] sha256(byte[] encoded) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new TrustAdministrationException(TrustAdministrationError.INTEGRITY_FAILURE, exception);
        }
    }

    private static WorkloadPrincipalLifecycle principalLifecycle(String value) {
        try { return WorkloadPrincipalLifecycle.valueOf(value); }
        catch (RuntimeException exception) { throw new TrustAdministrationException(TrustAdministrationError.INTEGRITY_FAILURE, exception); }
    }

    private static WorkloadCredentialLifecycle credentialLifecycle(String value) {
        try { return WorkloadCredentialLifecycle.valueOf(value); }
        catch (RuntimeException exception) { throw new TrustAdministrationException(TrustAdministrationError.INTEGRITY_FAILURE, exception); }
    }

    private static Timestamp timestamp(Instant value) { return Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }

    private static TrustAdministrationException failure(TrustAdministrationError error) {
        return new TrustAdministrationException(error);
    }

    private static TrustAdministrationException persistence(Throwable cause) {
        return new TrustAdministrationException(TrustAdministrationError.PERSISTENCE_FAILURE, cause);
    }

    private record PrincipalIdentityRow(UUID id, String principalId, String issuer) { }
    private record ExistingCredentialRow(UUID id, String algorithm, byte[] fingerprint, Instant notBefore,
                                         Instant notAfter, String lifecycleStatus) { }
}
