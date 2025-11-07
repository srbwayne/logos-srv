package com.josecjuniors.logossrv.adapters.out.registroatividadedetalhe.jpa;

import com.josecjuniors.logossrv.core.registroatividadedetalhe.domain.model.RegistroAtividadeDetalhe;
import com.josecjuniors.logossrv.core.registroatividadedetalhe.domain.model.RegistroAtividadeDetalheId;
import com.josecjuniors.logossrv.core.registroatividadedetalhe.domain.repository.RegistroAtividadeDetalheRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAtividadeDetalheJpaRepository extends RegistroAtividadeDetalheRepository, JpaRepository<RegistroAtividadeDetalhe, RegistroAtividadeDetalheId> {}
