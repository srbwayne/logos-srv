package com.josecjuniors.logossrv.core.atributo.domain.repository;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;

import java.util.List;
import java.util.Optional;

public interface AtributoRepository {
    Atributo save(Atributo atributo);
    Optional<Atributo> findById(AtributoId id);
    List<Atributo> findAll();
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, AtributoId id);
}
