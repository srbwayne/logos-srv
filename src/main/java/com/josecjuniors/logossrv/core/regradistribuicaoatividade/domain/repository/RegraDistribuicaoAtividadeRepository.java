package com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;

import java.util.List;
import java.util.Optional;

public interface RegraDistribuicaoAtividadeRepository {
    RegraDistribuicaoAtividade save(RegraDistribuicaoAtividade regra);
    Optional<RegraDistribuicaoAtividade> findById(RegraDistribuicaoAtividadeId id);
    List<RegraDistribuicaoAtividade> findByAtividadeConfigId(AtividadeConfigId atividadeId);
    void deleteById(RegraDistribuicaoAtividadeId id);
    void deleteAll(); // Adicionado
}
