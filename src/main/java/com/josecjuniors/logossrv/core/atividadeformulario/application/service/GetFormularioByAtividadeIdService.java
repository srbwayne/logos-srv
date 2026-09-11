package com.josecjuniors.logossrv.core.atividadeformulario.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.application.port.in.GetFormularioByAtividadeIdUseCase;
import com.josecjuniors.logossrv.core.atividadeformulario.application.dto.AtividadeFormularioDto;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.repository.AtividadeFormularioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetFormularioByAtividadeIdService implements GetFormularioByAtividadeIdUseCase {

    private final AtividadeFormularioRepository repository;

    public GetFormularioByAtividadeIdService(AtividadeFormularioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AtividadeFormularioDto> getByAtividadeId(AtividadeConfigId atividadeId) {
        return repository.findByAtividadeConfigId(atividadeId)
                .map(formulario -> new AtividadeFormularioDto(formulario.getFormularioJson(), formulario.getVersao()));
    }
}
