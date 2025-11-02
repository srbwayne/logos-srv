package com.josecjuniors.logossrv.core.estresse.domain.repository;

import com.josecjuniors.logossrv.core.estresse.domain.model.Estresse;
import com.josecjuniors.logossrv.core.estresse.domain.model.EstresseId;

import java.util.Optional;

public interface EstresseRepository {

    Estresse save(Estresse estresse);

    Optional<Estresse> findById(EstresseId id);

    void delete(Estresse estresse);
}
