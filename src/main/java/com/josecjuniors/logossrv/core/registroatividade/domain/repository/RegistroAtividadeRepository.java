package com.josecjuniors.logossrv.core.registroatividade.domain.repository;

import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;

import java.util.List;
import java.util.Optional;

public interface RegistroAtividadeRepository {
    RegistroAtividade save(RegistroAtividade registro);
    Optional<RegistroAtividade> findById(RegistroAtividadeId id);
    Optional<RegistroAtividade> findByIdWithDetails(RegistroAtividadeId id);
    void deleteAll();
    List<RegistroAtividade> findAll();
}
