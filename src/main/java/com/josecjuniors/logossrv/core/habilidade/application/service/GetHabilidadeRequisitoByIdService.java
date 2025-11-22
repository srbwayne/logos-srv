package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.BuscarHabilidadeRequisitoPorIdCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.GetHabilidadeRequisitoByIdUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeRequisitoNaoEncontradoException;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.RequisitoNaoPertenceAEstaHabilidadeException;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto.fromDomain;

@Service
@Transactional(readOnly = true)
public class GetHabilidadeRequisitoByIdService implements GetHabilidadeRequisitoByIdUseCase {

    private final HabilidadeRequisitoRepository habilidadeRequisitoRepository;

    public GetHabilidadeRequisitoByIdService(HabilidadeRequisitoRepository habilidadeRequisitoRepository) {
        this.habilidadeRequisitoRepository = habilidadeRequisitoRepository;
    }

    @Override
    public Optional<GetHabilidadeRequisitoByIdDto> getById(BuscarHabilidadeRequisitoPorIdCommand command) {
        HabilidadeRequisito requisito = habilidadeRequisitoRepository.findById(command.habilidadeRequisitoId())
                .orElseThrow(HabilidadeRequisitoNaoEncontradoException::new);

        if (!requisito.getHabilidade().getId().equals(command.habilidadeId())) {
            throw new RequisitoNaoPertenceAEstaHabilidadeException();
        }

        return Optional.of(fromDomain(requisito));
    }
}
