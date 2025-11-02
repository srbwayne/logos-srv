package com.josecjuniors.logossrv.adapters.out.estresseglobal.jpa;

import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.estresseglobal.domain.repository.EstresseGlobalRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstresseGlobalJpaRepository extends EstresseGlobalRepository, JpaRepository<EstresseGlobal, EstresseGlobalId> {
}
