package com.josecjuniors.logossrv.adapters.out.progression;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgressionExternalExecutionJpaRepository extends JpaRepository<ProgressionExternalExecutionEntity, UUID> {
    Optional<ProgressionExternalExecutionEntity> findBySourceSystemAndIdempotencyKey(String sourceSystem, String idempotencyKey);
}
