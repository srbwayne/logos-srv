package com.josecjuniors.logossrv.adapters.out.registroatividade.jpa;


import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeDetalhe;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeDetalheId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeDetalheRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAtividadeDetalheJpaRepository extends RegistroAtividadeDetalheRepository, JpaRepository<RegistroAtividadeDetalhe, RegistroAtividadeDetalheId> {}
