package com.josecjuniors.logossrv.adapters.out.debuff.jpa;

import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebuffJpaRepository extends DebuffRepository, JpaRepository<Debuff, DebuffId> {
    @Override
    Debuff save(Debuff debuff);

    @Override
    Optional<Debuff> findById(DebuffId id);

    @Override
    @Query("SELECT d FROM Debuff d WHERE " +
           "(:searchTerm IS NULL OR d.nome LIKE %:searchTerm%)")
    Page<Debuff> findByNomeContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Override
    List<Debuff> findAll();

    @Override
    boolean existsByNome(String nome);

    @Override
    boolean existsByNomeAndIdNot(String nome, DebuffId id);

    @Override
    void deleteAll();
}
