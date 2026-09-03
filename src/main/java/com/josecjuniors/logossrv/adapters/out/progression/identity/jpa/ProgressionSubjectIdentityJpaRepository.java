package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressionSubjectIdentityJpaRepository
        extends JpaRepository<ProgressionSubjectIdentity, UUID> {

    Optional<ProgressionSubjectIdentity> findByNamespaceAndExternalId(String namespace, String externalId);
}
