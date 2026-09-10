package com.josecjuniors.logossrv.adapters.out.fatorcalculo.jpa;

import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FatorCalculoJpaRepository extends FatorCalculoRepository, JpaRepository<FatorCalculo, FatorCalculoId> {

    @Override
    FatorCalculo save(FatorCalculo fatorCalculo);

    @Override
    Optional<FatorCalculo> findById(FatorCalculoId id);

    @Override
    Page<FatorCalculo> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, FatorCalculoId id);

    @Override
    boolean existsBySemanticKey(String semanticKey);

    @Override
    boolean existsBySemanticKeyAndIdNot(String semanticKey, FatorCalculoId id);

    @Override
    Optional<FatorCalculo> findBySemanticKey(String semanticKey);

    @Override
    void deleteAll();
}
