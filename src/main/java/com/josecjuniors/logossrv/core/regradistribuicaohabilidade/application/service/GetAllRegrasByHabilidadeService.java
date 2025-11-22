package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in.GetAllRegrasByHabilidadeUseCase;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.repository.RegraDistribuicaoHabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAllRegrasByHabilidadeService implements GetAllRegrasByHabilidadeUseCase {

    private final RegraDistribuicaoHabilidadeRepository repository;

    public GetAllRegrasByHabilidadeService(RegraDistribuicaoHabilidadeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RegraDistribuicaoHabilidadeDto> getAllByHabilidade(HabilidadeId habilidadeId) {
        return repository.findAllByHabilidadeId(habilidadeId).stream()
                .map(RegraDistribuicaoHabilidadeDto::fromDomain)
                .collect(Collectors.toList());
    }
}
