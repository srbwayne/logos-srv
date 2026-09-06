package com.josecjuniors.logossrv.adapters.out.progression;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ProgressionExternalExecutionJpaRepository extends JpaRepository<ProgressionExternalExecutionEntity, UUID> {
    Optional<ProgressionExternalExecutionEntity> findBySourceSystemAndIdempotencyKey(String sourceSystem, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ProgressionExternalExecutionEntity e where e.sourceSystem = :sourceSystem and e.idempotencyKey = :idempotencyKey")
    Optional<ProgressionExternalExecutionEntity> findBySourceSystemAndIdempotencyKeyForUpdate(@Param("sourceSystem") String sourceSystem,
                                                                                                @Param("idempotencyKey") String idempotencyKey);

    List<ProgressionExternalExecutionEntity> findByProcessingStatusInOrderByCreatedAtAsc(List<String> statuses);
}
