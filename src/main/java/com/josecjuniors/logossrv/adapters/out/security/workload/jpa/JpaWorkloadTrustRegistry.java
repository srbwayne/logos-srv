package com.josecjuniors.logossrv.adapters.out.security.workload.jpa;

import com.josecjuniors.logossrv.adapters.out.security.workload.crypto.SpkiPemWorkloadPublicKeyParser;
import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadTrustRegistryIntegrityException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadTrustRegistry;
import com.josecjuniors.logossrv.core.security.workload.domain.TrustedWorkloadCredential;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadCredentialLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadKeyId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalId;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadPrincipalLifecycle;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadSignatureAlgorithm;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Repository
public class JpaWorkloadTrustRegistry implements WorkloadTrustRegistry {
    private final WorkloadSigningKeyReadRepository repository;
    private final SpkiPemWorkloadPublicKeyParser publicKeyParser;

    public JpaWorkloadTrustRegistry(WorkloadSigningKeyReadRepository repository,
                                    SpkiPemWorkloadPublicKeyParser publicKeyParser) {
        this.repository = repository;
        this.publicKeyParser = publicKeyParser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrustedWorkloadCredential> findByIssuerAndKid(WorkloadIssuer issuer, WorkloadKeyId kid) {
        if (issuer == null || kid == null) {
            throw new IllegalArgumentException("issuer and kid are required");
        }
        return repository.findByIssuerAndKid(issuer.value(), kid.value()).map(entity -> map(entity, issuer, kid));
    }

    private TrustedWorkloadCredential map(WorkloadSigningKeyEntity key, WorkloadIssuer requestedIssuer,
                                          WorkloadKeyId requestedKid) {
        WorkloadPrincipalEntity principal = key.getPrincipal();
        try {
            require(principal != null, "principal relation is missing");
            require(principal.getId() != null && key.getId() != null, "database identity is missing");
            require(principal.getCreatedAt() != null && principal.getUpdatedAt() != null,
                    "principal audit timestamps are missing");
            require(key.getCreatedAt() != null && key.getUpdatedAt() != null,
                    "credential audit timestamps are missing");
            require("WORKLOAD".equals(principal.getPrincipalType()), "principal type is unexpected");
            require(requestedIssuer.value().equals(principal.getIssuer()), "issuer lookup is incoherent");
            require(requestedKid.value().equals(key.getKid()), "key id lookup is incoherent");
            require(key.getNotBefore() != null, "credential not-before timestamp is missing");
            require(key.getPublicKeySpkiSha256() != null && key.getPublicKeySpkiSha256().length == 32,
                    "public key fingerprint is invalid");

            WorkloadSignatureAlgorithm algorithm = parseAlgorithm(key.getAlgorithm());
            WorkloadPrincipalLifecycle principalLifecycle = parsePrincipalLifecycle(principal.getLifecycleStatus());
            WorkloadCredentialLifecycle credentialLifecycle = parseCredentialLifecycle(key.getLifecycleStatus());
            var publicKey = publicKeyParser.parseP256(key.getPublicKeyPem());
            byte[] actualFingerprint = sha256(publicKey.getEncoded());
            require(MessageDigest.isEqual(actualFingerprint, key.getPublicKeySpkiSha256()),
                    "public key fingerprint does not match SPKI material");

            return new TrustedWorkloadCredential(
                    principal.getId(), key.getId(), new WorkloadIssuer(principal.getIssuer()),
                    new WorkloadPrincipalId(principal.getPrincipalId()), principalLifecycle,
                    principal.getDisabledAt(), principal.getRevokedAt(),
                    new WorkloadKeyId(key.getKid()), algorithm, publicKey, credentialLifecycle,
                    key.getNotBefore(), key.getNotAfter(), key.getActivatedAt(), key.getRevokedAt(), key.getRetiredAt());
        } catch (WorkloadTrustRegistryIntegrityException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new WorkloadTrustRegistryIntegrityException("registered credential record is incoherent", exception);
        }
    }

    private static WorkloadSignatureAlgorithm parseAlgorithm(String value) {
        if (!WorkloadSignatureAlgorithm.ES256.name().equals(value)) {
            throw integrity("registered algorithm is unsupported");
        }
        return WorkloadSignatureAlgorithm.ES256;
    }

    private static WorkloadPrincipalLifecycle parsePrincipalLifecycle(String value) {
        try {
            return WorkloadPrincipalLifecycle.valueOf(value);
        } catch (RuntimeException exception) {
            throw new WorkloadTrustRegistryIntegrityException("principal lifecycle is unknown", exception);
        }
    }

    private static WorkloadCredentialLifecycle parseCredentialLifecycle(String value) {
        try {
            return WorkloadCredentialLifecycle.valueOf(value);
        } catch (RuntimeException exception) {
            throw new WorkloadTrustRegistryIntegrityException("credential lifecycle is unknown", exception);
        }
    }

    private static byte[] sha256(byte[] encodedKey) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(encodedKey);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JCA does not provide SHA-256", exception);
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw integrity(reason);
        }
    }

    private static WorkloadTrustRegistryIntegrityException integrity(String reason) {
        return new WorkloadTrustRegistryIntegrityException(reason);
    }
}
