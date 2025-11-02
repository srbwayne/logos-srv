package com.josecjuniors.logossrv.adapters.out.regrafatorxp.jpa;

import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegraFatorXPJpaRepository extends RegraFatorXPRepository, JpaRepository<RegraFatorXP, RegraFatorXPId> {}
