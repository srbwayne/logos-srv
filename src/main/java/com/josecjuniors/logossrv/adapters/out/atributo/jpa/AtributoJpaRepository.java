package com.josecjuniors.logossrv.adapters.out.atributo.jpa;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtributoJpaRepository extends AtributoRepository, JpaRepository<Atributo, AtributoId> {

    @Override
    Atributo save(Atributo atributo);

    @Override
    Optional<Atributo> findById(AtributoId id);

    @Override
    Page<Atributo> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, AtributoId id);

    @Override
    void deleteById(AtributoId id);

    @Override
    void deleteAll();

    @Override
    List<Atributo> findAll();

    @Override
    default List<Atributo> saveAll(List<Atributo> atributos){
        return saveAllAndFlush(atributos);
    };
}
