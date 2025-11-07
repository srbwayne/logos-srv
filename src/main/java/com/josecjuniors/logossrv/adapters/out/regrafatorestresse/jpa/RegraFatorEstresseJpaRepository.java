package com.josecjuniors.logossrv.adapters.out.regrafatorestresse.jpa;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegraFatorEstresseJpaRepository extends RegraFatorEstresseRepository, JpaRepository<RegraFatorEstresse, RegraFatorEstresseId> {}
