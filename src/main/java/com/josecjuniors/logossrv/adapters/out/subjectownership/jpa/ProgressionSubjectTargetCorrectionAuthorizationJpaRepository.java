package com.josecjuniors.logossrv.adapters.out.subjectownership.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence-only repository; TC-R1 adds no application approval service. */
@Repository
public interface ProgressionSubjectTargetCorrectionAuthorizationJpaRepository
        extends JpaRepository<ProgressionSubjectTargetCorrectionAuthorization, UUID> {

    Optional<ProgressionSubjectTargetCorrectionAuthorization> findByCorrectionRequestId(UUID correctionRequestId);
}
