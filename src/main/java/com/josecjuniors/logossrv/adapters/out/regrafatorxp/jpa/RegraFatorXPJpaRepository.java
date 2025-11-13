package com.josecjuniors.logossrv.adapters.out.regrafatorxp.jpa;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegraFatorXPJpaRepository extends RegraFatorXPRepository, JpaRepository<RegraFatorXP, RegraFatorXPId> {

    @Override
    @Query("SELECT DISTINCT rfx FROM RegraFatorXP rfx " +
           "JOIN FETCH rfx.fatorCalculo " +
           "JOIN rfx.regraDistribuicaoAtividade rda " +
           "WHERE rda.atividadeConfig.id = :atividadeConfigId")
    List<RegraFatorXP> findRegrasByAtividadeConfigId(@Param("atividadeConfigId") AtividadeConfigId atividadeConfigId);

    @Override
    @Query("SELECT rfx FROM RegraFatorXP rfx JOIN FETCH rfx.fatorCalculo WHERE rfx.id = :id")
    Optional<RegraFatorXP> findById(@Param("id") RegraFatorXPId id);
}
