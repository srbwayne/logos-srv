package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in.GetAllRegrasByAtividadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAllRegrasByAtividadeService implements GetAllRegrasByAtividadeUseCase {

    private final RegraDistribuicaoAtividadeRepository repository;

    public GetAllRegrasByAtividadeService(RegraDistribuicaoAtividadeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RegraDistribuicaoAtividadeDto> getAllByAtividade(AtividadeConfigId atividadeId) {
        return repository.findByAtividadeConfigId(atividadeId).stream()
                .map(RegraDistribuicaoAtividadeDto::fromDomain)
                .collect(Collectors.toList());
    }
}
