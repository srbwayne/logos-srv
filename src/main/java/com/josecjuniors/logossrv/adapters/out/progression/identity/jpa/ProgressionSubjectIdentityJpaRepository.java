package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import java.util.Optional;
import java.util.UUID;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressionSubjectIdentityJpaRepository
        extends JpaRepository<ProgressionSubjectIdentity, UUID> {

    Optional<ProgressionSubjectIdentity> findByNamespaceAndExternalId(String namespace, String externalId);

    @Query("select identity.jogador.user.id from ProgressionSubjectIdentity identity "
            + "where identity.namespace = :namespace and identity.externalId = :externalId")
    Optional<AppUserId> findAppUserIdByNamespaceAndExternalId(@Param("namespace") String namespace,
                                                               @Param("externalId") String externalId);
}
