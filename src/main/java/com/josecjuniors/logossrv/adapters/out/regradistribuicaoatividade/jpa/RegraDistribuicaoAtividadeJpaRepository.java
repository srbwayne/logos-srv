package com.josecjuniors.logossrv.adapters.out.regradistribuicaoatividade.jpa;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegraDistribuicaoAtividadeJpaRepository extends RegraDistribuicaoAtividadeRepository, JpaRepository<RegraDistribuicaoAtividade, RegraDistribuicaoAtividadeId> {

    @Override
    RegraDistribuicaoAtividade save(RegraDistribuicaoAtividade regra);

    @Override
    @Query("SELECT r FROM RegraDistribuicaoAtividade r JOIN FETCH r.atividadeConfig JOIN FETCH r.atributo WHERE r.id = :id")
    Optional<RegraDistribuicaoAtividade> findById(@Param("id") RegraDistribuicaoAtividadeId id);

    @Override
    @Query("SELECT r FROM RegraDistribuicaoAtividade r JOIN FETCH r.atividadeConfig JOIN FETCH r.atributo WHERE r.atividadeConfig.id = :atividadeId")
    List<RegraDistribuicaoAtividade> findByAtividadeConfigId(@Param("atividadeId") AtividadeConfigId atividadeId);

    @Override
    void deleteById(RegraDistribuicaoAtividadeId id);

    @Override
    void deleteAll(); // Adicionado
}
