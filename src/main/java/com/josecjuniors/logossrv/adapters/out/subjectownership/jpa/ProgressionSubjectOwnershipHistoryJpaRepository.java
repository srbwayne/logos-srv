package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface ProgressionSubjectOwnershipHistoryJpaRepository
        extends JpaRepository<ProgressionSubjectOwnershipHistoryEntry, UUID> {
    Optional<ProgressionSubjectOwnershipHistoryEntry> findByIdentityIdAndAggregateVersion(
            UUID identityId, long aggregateVersion);
}
