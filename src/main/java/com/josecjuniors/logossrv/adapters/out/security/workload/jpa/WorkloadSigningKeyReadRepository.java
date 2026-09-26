package com.josecjuniors.logossrv.adapters.out.security.workload.jpa;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkloadSigningKeyReadRepository extends Repository<WorkloadSigningKeyEntity, UUID> {
    @Query("select k from WorkloadSigningKeyEntity k join fetch k.principal p "
            + "where p.issuer = :issuer and k.kid = :kid")
    Optional<WorkloadSigningKeyEntity> findByIssuerAndKid(@Param("issuer") String issuer,
                                                          @Param("kid") String kid);
}
