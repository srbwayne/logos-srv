package com.josecjuniors.logossrv.core.registroatividade.domain.repository;

import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;

import java.util.Optional;

public interface RegistroAtividadeRepository {

    RegistroAtividade save(RegistroAtividade registroAtividade);

    Optional<RegistroAtividade> findById(RegistroAtividadeId id);

    void delete(RegistroAtividade registroAtividade);
}
