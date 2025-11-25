package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.command.UpdateVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.VicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.UpdateVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.VicioJaExisteException;
import com.josecjuniors.logossrv.core.vicio.domain.exception.VicioNaoEncontradoException;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateVicioService implements UpdateVicioUseCase {

    private final VicioRepository repository;

    public UpdateVicioService(VicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public VicioDto update(UpdateVicioCommand command) {
        Vicio vicio = repository.findById(command.id())
                .orElseThrow(VicioNaoEncontradoException::new);

        if (repository.existsByNomeAndIdNot(command.nome(), command.id())) {
            throw new VicioJaExisteException();
        }

        vicio.atualizar(command.nome(), command.descricao());
        Vicio vicioSalvo = repository.save(vicio);
        return VicioDto.fromDomain(vicioSalvo);
    }
}
