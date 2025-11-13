package com.josecjuniors.logossrv.core.atributo.domain.repository;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AtributoRepository {
    Atributo save(Atributo atributo);
    Optional<Atributo> findById(AtributoId id);
    Page<Atributo> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, AtributoId id);
    void deleteById(AtributoId id);
    void deleteAll();
    List<Atributo> findAll();
    List<Atributo> saveAll(List<Atributo> atributos); // Adicionado
}
