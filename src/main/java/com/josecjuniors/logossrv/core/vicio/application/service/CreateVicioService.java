package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.command.CreateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.CreateVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.VicioJaExisteException;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateVicioService implements CreateVicioUseCase {

    private final VicioRepository repository;

    public CreateVicioService(VicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public VicioDto create(CreateVicioCommand command) {
        if (repository.existsByNome(command.nome())) {
            throw new VicioJaExisteException();
        }
        var vicio = new Vicio(VicioId.generate(), command.nome(), command.descricao());
        var vicioSalvo = repository.save(vicio);
        return VicioDto.fromDomain(vicioSalvo);
    }
}
