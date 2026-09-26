package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProgressionSubjectLinkChallengeJpaRepository extends JpaRepository<ProgressionSubjectLinkChallengeEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProgressionSubjectLinkChallengeEntity c where c.tokenHash = :tokenHash")
    Optional<ProgressionSubjectLinkChallengeEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
