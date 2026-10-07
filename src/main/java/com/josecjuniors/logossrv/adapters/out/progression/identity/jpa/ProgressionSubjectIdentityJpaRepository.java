package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface ProgressionSubjectIdentityJpaRepository
        extends JpaRepository<ProgressionSubjectIdentity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select identity from ProgressionSubjectIdentity identity where identity.id = :identityId")
    Optional<ProgressionSubjectIdentity> findCurrentIdentityByIdForUpdate(
            @Param("identityId") UUID identityId);

    @Query(value = """
            SELECT pointer.current_identity_id
            FROM progression_subject_current_binding pointer
            WHERE pointer.namespace = :namespace AND pointer.external_id = :externalId
            FOR UPDATE
            """, nativeQuery = true)
    Optional<UUID> lockCurrentIdentityId(@Param("namespace") String namespace,
                                         @Param("externalId") String externalId);

    @Query(value = """
            SELECT identity.id
            FROM progression_subject_current_binding pointer
            JOIN progression_subject_identity identity
              ON identity.id = pointer.current_identity_id
             AND identity.namespace = pointer.namespace
             AND identity.external_id = pointer.external_id
            WHERE pointer.namespace = :namespace AND pointer.external_id = :externalId
            """, nativeQuery = true)
    Optional<UUID> findCurrentIdentityId(@Param("namespace") String namespace,
                                         @Param("externalId") String externalId);

    @Query(value = """
            SELECT app_user.id
            FROM progression_subject_current_binding pointer
            JOIN progression_subject_identity identity
              ON identity.id = pointer.current_identity_id
             AND identity.namespace = pointer.namespace
             AND identity.external_id = pointer.external_id
            JOIN jogador ON jogador.id = identity.jogador_id
            JOIN app_user ON app_user.id = jogador.user_id
            WHERE pointer.namespace = :namespace AND pointer.external_id = :externalId
            """, nativeQuery = true)
    Optional<UUID> findCurrentAppUserId(@Param("namespace") String namespace,
                                        @Param("externalId") String externalId);

    boolean existsByNamespaceAndExternalId(String namespace, String externalId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO progression_subject_current_binding (namespace, external_id, current_identity_id)
            VALUES (:namespace, :externalId, :identityId)
            ON CONFLICT (namespace, external_id) DO NOTHING
            """, nativeQuery = true)
    int insertCurrentBindingIfAbsent(@Param("namespace") String namespace,
                                     @Param("externalId") String externalId,
                                     @Param("identityId") UUID identityId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO progression_subject_identity
                (id, namespace, external_id, jogador_id, identity_class, ownership_status,
                 verification_status, ownership_version)
            VALUES (:id, :namespace, :externalId, :jogadorId, :identityClass, :ownershipStatus,
                    :verificationStatus, 0)
            """, nativeQuery = true)
    int insertIdentity(@Param("id") UUID id,
                       @Param("namespace") String namespace,
                       @Param("externalId") String externalId,
                       @Param("jogadorId") UUID jogadorId,
                       @Param("identityClass") String identityClass,
                       @Param("ownershipStatus") String ownershipStatus,
                       @Param("verificationStatus") String verificationStatus);
}
