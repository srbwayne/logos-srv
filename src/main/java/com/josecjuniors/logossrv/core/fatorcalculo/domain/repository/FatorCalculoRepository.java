package com.josecjuniors.logossrv.core.fatorcalculo.domain.repository;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FatorCalculoRepository {
    FatorCalculo save(FatorCalculo fatorCalculo);
    Optional<FatorCalculo> findById(FatorCalculoId id);
    Page<FatorCalculo> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, FatorCalculoId id);
    void deleteById(FatorCalculoId id);
    void deleteAll();
}
