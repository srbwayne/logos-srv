package com.josecjuniors.logossrv.core.estresseglobal.domain.repository;

import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;

import java.util.Optional;

public interface EstresseGlobalRepository {

    EstresseGlobal save(EstresseGlobal estresseGlobal);

    Optional<EstresseGlobal> findById(EstresseGlobalId id);

    void delete(EstresseGlobal estresseGlobal);
}
