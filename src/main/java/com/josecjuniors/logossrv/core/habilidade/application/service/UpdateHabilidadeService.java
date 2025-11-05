package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.UpdateHabilidadeUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeJaExisteException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateHabilidadeService implements UpdateHabilidadeUseCase {

    private final HabilidadeRepository habilidadeRepository;

    public UpdateHabilidadeService(HabilidadeRepository habilidadeRepository) {
        this.habilidadeRepository = habilidadeRepository;
    }

    @Override
    public HabilidadeDto updateHabilidade(UpdateHabilidadeCommand command) {
        Habilidade habilidade = habilidadeRepository.findById(command.habilidadeId())
                .orElseThrow(HabilidadeNaoEncontradaException::new);

        if (habilidadeRepository.existsByNomeAndIdNot(command.novoNome(), command.habilidadeId())) {
            throw new HabilidadeJaExisteException(command.novoNome());
        }

        habilidade.atualizarNome(command.novoNome());
        habilidade.atualizarDescricao(command.novaDescricao());
        Habilidade habilidadeAtualizada = habilidadeRepository.save(habilidade);

        return toDto(habilidadeAtualizada);
    }

    private HabilidadeDto toDto(Habilidade habilidade) {
        return new HabilidadeDto(
                habilidade.getId().getValue().toString(),
                habilidade.getNome(),
                habilidade.getDescricao()
        );
    }
}
