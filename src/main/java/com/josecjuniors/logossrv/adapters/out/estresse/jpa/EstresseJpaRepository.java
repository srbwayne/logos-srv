package com.josecjuniors.logossrv.adapters.out.estresse.jpa;

import com.josecjuniors.logossrv.core.estresse.domain.model.Estresse;
import com.josecjuniors.logossrv.core.estresse.domain.model.EstresseId;
import com.josecjuniors.logossrv.core.estresse.domain.repository.EstresseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstresseJpaRepository extends EstresseRepository, JpaRepository<Estresse, EstresseId> {
}
