package com.josecjuniors.logossrv.adapters.out.debuff.jpa;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegraDistribuicaoDebuffJpaRepository extends RegraDistribuicaoDebuffRepository, JpaRepository<RegraDistribuicaoDebuff, RegraDistribuicaoDebuffId> {

    @Override
    RegraDistribuicaoDebuff save(RegraDistribuicaoDebuff regra);

    @Override
    void delete(RegraDistribuicaoDebuff regra);

    @Override
    @Query("SELECT r FROM RegraDistribuicaoDebuff r JOIN FETCH r.debuff JOIN FETCH r.atributo " +
           "WHERE r.debuff.id = :debuffId " +
           "AND (COALESCE(:atributoNome, NULL) IS NULL OR r.atributo.nome LIKE %:atributoNome%)"
    )
    Page<RegraDistribuicaoDebuff> findAllByDebuffId(@Param("debuffId") DebuffId debuffId, @Param("atributoNome") String atributoNome, Pageable pageable);

    @Override
    @Query("SELECT r FROM RegraDistribuicaoDebuff r JOIN FETCH r.debuff JOIN FETCH r.atributo WHERE r.id = :id")
    Optional<RegraDistribuicaoDebuff> findById(@Param("id") RegraDistribuicaoDebuffId id);

    @Override
    void deleteAll();
}
