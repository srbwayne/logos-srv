package com.josecjuniors.logossrv.adapters.out.habilidade.jpa;

import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HabilidadeJpaRepository extends HabilidadeRepository, JpaRepository<Habilidade, HabilidadeId> {
    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, HabilidadeId id);
}
