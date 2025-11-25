package com.josecjuniors.logossrv.core.debuff.domain.repository;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RegraDistribuicaoDebuffRepository {
    RegraDistribuicaoDebuff save(RegraDistribuicaoDebuff regra);
    void delete(RegraDistribuicaoDebuff regra);
    Optional<RegraDistribuicaoDebuff> findById(RegraDistribuicaoDebuffId id);
    Page<RegraDistribuicaoDebuff> findAllByDebuffId(DebuffId debuffId, String atributoNome, Pageable pageable);

    void deleteAll();
}
