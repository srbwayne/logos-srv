package com.josecjuniors.logossrv.adapters.out.atributo.jpa;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtributoJpaRepository extends AtributoRepository, JpaRepository<Atributo, AtributoId> {
    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, AtributoId id);
}
