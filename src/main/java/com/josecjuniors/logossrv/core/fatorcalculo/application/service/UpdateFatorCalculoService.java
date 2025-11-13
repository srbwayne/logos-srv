package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.UpdateFatorCalculoCommand;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.UpdateFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoJaExisteException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateFatorCalculoService implements UpdateFatorCalculoUseCase {

    private final FatorCalculoRepository repository;

    public UpdateFatorCalculoService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public FatorCalculoDto update(UpdateFatorCalculoCommand command) {
        FatorCalculo fator = repository.findById(command.fatorCalculoId())
                .orElseThrow(FatorCalculoNaoEncontradoException::new);

        if (repository.existsByNomeAndIdNot(command.nome(), command.fatorCalculoId())) {
            throw new FatorCalculoJaExisteException(command.nome());
        }

        fator.atualizar(command.nome(), command.unidadeMedida(), command.tipoInput());
        FatorCalculo fatorAtualizado = repository.save(fator);
        return FatorCalculoDto.fromDomain(fatorAtualizado);
    }
}
