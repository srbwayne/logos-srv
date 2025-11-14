package com.josecjuniors.logossrv.adapters.out.regrafatorestresse.jpa;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegraFatorEstresseJpaRepository extends RegraFatorEstresseRepository, JpaRepository<RegraFatorEstresse, RegraFatorEstresseId> {

    @Override
    RegraFatorEstresse save(RegraFatorEstresse regra);

    @Override
    Optional<RegraFatorEstresse> findById(RegraFatorEstresseId id);

    @Override
    void deleteById(RegraFatorEstresseId id);

    @Override
    void deleteAll();
}
