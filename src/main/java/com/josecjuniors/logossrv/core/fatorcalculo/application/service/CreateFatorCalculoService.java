package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.CreateFatorCalculoCommand;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.CreateFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoJaExisteException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateFatorCalculoService implements CreateFatorCalculoUseCase {

    private final FatorCalculoRepository repository;

    public CreateFatorCalculoService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public FatorCalculoDto create(CreateFatorCalculoCommand command) {
        if (repository.existsByNome(command.nome())) {
            throw new FatorCalculoJaExisteException(command.nome());
        }
        FatorCalculo novoFator = new FatorCalculo(FatorCalculoId.generate(), command.nome(), command.unidadeMedida(), command.tipoInput());
        FatorCalculo fatorSalvo = repository.save(novoFator);
        return FatorCalculoDto.fromDomain(fatorSalvo);
    }
}
