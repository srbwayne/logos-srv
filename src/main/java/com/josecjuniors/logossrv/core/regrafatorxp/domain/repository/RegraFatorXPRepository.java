package com.josecjuniors.logossrv.core.regrafatorxp.domain.repository;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;

import java.util.List;
import java.util.Optional;

public interface RegraFatorXPRepository {
    RegraFatorXP save(RegraFatorXP regra);
    List<RegraFatorXP> findRegrasByAtividadeConfigId(AtividadeConfigId atividadeConfigId);
    Optional<RegraFatorXP> findById(RegraFatorXPId id);
    void deleteById(RegraFatorXPId id);
    void deleteAll();
}
