package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetRequisitosDaHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRequisitosDaHabilidadeService implements GetRequisitosDaHabilidadeUseCase {

    private final HabilidadeRequisitoRepository habilidadeRequisitoRepository;

    public GetRequisitosDaHabilidadeService(HabilidadeRequisitoRepository habilidadeRequisitoRepository) {
        this.habilidadeRequisitoRepository = habilidadeRequisitoRepository;
    }

    @Override
    public List<HabilidadeRequisitoDto> getRequisitos(HabilidadeId habilidadeId) {
        return habilidadeRequisitoRepository.findByHabilidadeId(habilidadeId)
                .stream()
                .map(HabilidadeRequisitoDto::fromDomain)
                .collect(Collectors.toList());
    }
}
