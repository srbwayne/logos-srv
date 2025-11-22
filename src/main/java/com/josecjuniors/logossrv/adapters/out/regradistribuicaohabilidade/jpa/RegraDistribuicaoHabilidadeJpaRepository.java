package com.josecjuniors.logossrv.adapters.out.regradistribuicaohabilidade.jpa;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegraDistribuicaoHabilidadeJpaRepository extends RegraDistribuicaoHabilidadeRepository, JpaRepository<RegraDistribuicaoHabilidade, RegraDistribuicaoHabilidadeId> {

    @Override
    @Query("SELECT r FROM RegraDistribuicaoHabilidade r JOIN FETCH r.habilidade JOIN FETCH r.atributo WHERE r.habilidade.id = :habilidadeId")
    List<RegraDistribuicaoHabilidade> findAllByHabilidadeId(@Param("habilidadeId") HabilidadeId habilidadeId);

    @Override
    @Query("SELECT r FROM RegraDistribuicaoHabilidade r JOIN FETCH r.habilidade JOIN FETCH r.atributo WHERE r.id = :id")
    Optional<RegraDistribuicaoHabilidade> findById(@Param("id") RegraDistribuicaoHabilidadeId id);
}
