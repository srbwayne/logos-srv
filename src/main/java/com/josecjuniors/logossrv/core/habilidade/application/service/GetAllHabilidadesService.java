package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetAllHabilidadesUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAllHabilidadesService implements GetAllHabilidadesUseCase {

    private final HabilidadeRepository habilidadeRepository;

    public GetAllHabilidadesService(HabilidadeRepository habilidadeRepository) {
        this.habilidadeRepository = habilidadeRepository;
    }

    @Override
    public List<HabilidadeDto> getAllHabilidades() {
        return habilidadeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private HabilidadeDto toDto(Habilidade habilidade) {
        return new HabilidadeDto(
                habilidade.getId().getValue().toString(),
                habilidade.getNome(),
                habilidade.getDescricao()
        );
    }
}
