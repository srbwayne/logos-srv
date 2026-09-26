package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import java.util.Optional;
import java.util.UUID;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressionSubjectIdentityJpaRepository
        extends JpaRepository<ProgressionSubjectIdentity, UUID> {

    Optional<ProgressionSubjectIdentity> findByNamespaceAndExternalId(String namespace, String externalId);

    @Query("select identity.jogador.user.id from ProgressionSubjectIdentity identity "
            + "where identity.namespace = :namespace and identity.externalId = :externalId "
            + "and identity.verificationStatus in ('LOGOS_NATIVE', 'INTEGRATION_VERIFIED')")
    Optional<AppUserId> findAppUserIdByNamespaceAndExternalId(@Param("namespace") String namespace,
                                                               @Param("externalId") String externalId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE progression_subject_identity
            SET jogador_id = :jogadorId,
                verification_status = 'INTEGRATION_VERIFIED',
                verified_at = :verifiedAt,
                verified_by_client_id = :clientId
            WHERE namespace = :namespace AND external_id = :externalId
              AND verification_status = 'UNVERIFIED'
            """, nativeQuery = true)
    int promoteUnverified(@Param("namespace") String namespace,
                          @Param("externalId") String externalId,
                          @Param("jogadorId") UUID jogadorId,
                          @Param("verifiedAt") java.time.Instant verifiedAt,
                          @Param("clientId") String clientId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO progression_subject_identity
                (id, namespace, external_id, jogador_id, verification_status, verified_at, verified_by_client_id)
            VALUES (:id, :namespace, :externalId, :jogadorId, 'INTEGRATION_VERIFIED', :verifiedAt, :clientId)
            ON CONFLICT (namespace, external_id) DO NOTHING
            """, nativeQuery = true)
    int insertVerifiedIfAbsent(@Param("id") UUID id,
                               @Param("namespace") String namespace,
                               @Param("externalId") String externalId,
                               @Param("jogadorId") UUID jogadorId,
                               @Param("verifiedAt") java.time.Instant verifiedAt,
                               @Param("clientId") String clientId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO progression_subject_identity (id, namespace, external_id, jogador_id, verification_status)
            VALUES (:id, :namespace, :externalId, :jogadorId,
                    CASE WHEN :namespace = 'logos-native' THEN 'LOGOS_NATIVE' ELSE 'UNVERIFIED' END)
            ON CONFLICT (namespace, external_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("namespace") String namespace,
                       @Param("externalId") String externalId,
                       @Param("jogadorId") UUID jogadorId);
}
