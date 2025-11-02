package com.josecjuniors.logossrv.adapters.out.registroatividade.jpa;

import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAtividadeJpaRepository extends RegistroAtividadeRepository, JpaRepository<RegistroAtividade, RegistroAtividadeId> {
}
