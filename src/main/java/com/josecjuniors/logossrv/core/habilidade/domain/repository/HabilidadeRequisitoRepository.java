package com.josecjuniors.logossrv.core.habilidade.domain.repository;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;

import java.util.List;
import java.util.Optional;

public interface HabilidadeRequisitoRepository {
    HabilidadeRequisito save(HabilidadeRequisito requisito);
    List<HabilidadeRequisito> findByHabilidadeId(HabilidadeId habilidadeId);
    Optional<HabilidadeRequisito> findById(HabilidadeRequisitoId requisitoId);
}
