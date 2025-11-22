package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.DeletarHabilidadeRequisitoCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.DeleteHabilidadeRequisitoUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeRequisitoNaoEncontradoException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.RequisitoNaoPertenceAEstaHabilidadeException;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteHabilidadeRequisitoService implements DeleteHabilidadeRequisitoUseCase {

    private final HabilidadeRequisitoRepository habilidadeRequisitoRepository;

    public DeleteHabilidadeRequisitoService(HabilidadeRequisitoRepository habilidadeRequisitoRepository) {
        this.habilidadeRequisitoRepository = habilidadeRequisitoRepository;
    }

    @Override
    public void delete(DeletarHabilidadeRequisitoCommand command) {
        HabilidadeRequisito requisito = habilidadeRequisitoRepository.findById(command.habilidadeRequisitoId())
                .orElseThrow(HabilidadeRequisitoNaoEncontradoException::new);

        if (!requisito.getHabilidade().getId().equals(command.habilidadeId())) {
            throw new RequisitoNaoPertenceAEstaHabilidadeException();
        }

        habilidadeRequisitoRepository.deleteById(command.habilidadeRequisitoId());
    }
}
