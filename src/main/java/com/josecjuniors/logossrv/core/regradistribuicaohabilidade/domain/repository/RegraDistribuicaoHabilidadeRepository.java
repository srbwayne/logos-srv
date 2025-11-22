package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidadeId;

import java.util.List;
import java.util.Optional;

public interface RegraDistribuicaoHabilidadeRepository {
    RegraDistribuicaoHabilidade save(RegraDistribuicaoHabilidade regra);
    void deleteById(RegraDistribuicaoHabilidadeId id);
    Optional<RegraDistribuicaoHabilidade> findById(RegraDistribuicaoHabilidadeId id);
    List<RegraDistribuicaoHabilidade> findAllByHabilidadeId(HabilidadeId habilidadeId);
    boolean existsById(RegraDistribuicaoHabilidadeId id);
    void deleteAll();
}
