package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeJaExisteException;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateHabilidadeService implements CreateHabilidadeUseCase {

    private final HabilidadeRepository habilidadeRepository;

    public CreateHabilidadeService(HabilidadeRepository habilidadeRepository) {
        this.habilidadeRepository = habilidadeRepository;
    }

    @Override
    public HabilidadeDto createHabilidade(CreateHabilidadeCommand command) {
        if (habilidadeRepository.existsByNome(command.nome())) {
            throw new HabilidadeJaExisteException(command.nome());
        }

        Habilidade novaHabilidade = new Habilidade(new HabilidadeId(), command.nome(), command.descricao());
        Habilidade habilidadeSalva = habilidadeRepository.save(novaHabilidade);

        return toDto(habilidadeSalva);
    }

    private HabilidadeDto toDto(Habilidade habilidade) {
        return new HabilidadeDto(
                habilidade.getId().getValue().toString(),
                habilidade.getNome(),
                habilidade.getDescricao()
        );
    }
}
