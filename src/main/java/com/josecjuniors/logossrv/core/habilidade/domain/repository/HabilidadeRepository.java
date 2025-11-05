package com.josecjuniors.logossrv.core.habilidade.domain.repository;

import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;

import java.util.List;
import java.util.Optional;

public interface HabilidadeRepository {
    Habilidade save(Habilidade habilidade);
    Optional<Habilidade> findById(HabilidadeId id);
    List<Habilidade> findAll();
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdNot(String nome, HabilidadeId id);
}
