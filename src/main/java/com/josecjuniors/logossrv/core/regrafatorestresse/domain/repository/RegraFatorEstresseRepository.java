package com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;

import java.util.Optional;

public interface RegraFatorEstresseRepository {
    RegraFatorEstresse save(RegraFatorEstresse regra);
    Optional<RegraFatorEstresse> findById(RegraFatorEstresseId id);
    void deleteById(RegraFatorEstresseId id);
    void deleteAll();
}
