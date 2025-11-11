package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetHabilidadeRequisitoByIdUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetHabilidadeRequisitoByIdService implements GetHabilidadeRequisitoByIdUseCase {

    private final HabilidadeRequisitoRepository habilidadeRequisitoRepository;

    public GetHabilidadeRequisitoByIdService(HabilidadeRequisitoRepository habilidadeRequisitoRepository) {
        this.habilidadeRequisitoRepository = habilidadeRequisitoRepository;
    }

    @Override
    public Optional<GetHabilidadeRequisitoByIdDto> getById(HabilidadeRequisitoId requisitoId) {
        return habilidadeRequisitoRepository.findById(requisitoId)
                .map(GetHabilidadeRequisitoByIdDto::fromDomain);
    }
}
