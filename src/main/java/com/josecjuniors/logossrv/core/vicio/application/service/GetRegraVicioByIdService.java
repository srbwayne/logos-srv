package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.command.BuscarRegravicioPorIdCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.GetRegraVicioByIdUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.RegraVicioNaoPertenceAoVicioException;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetRegraVicioByIdService implements GetRegraVicioByIdUseCase {

    private final RegraVicioRepository repository;

    public GetRegraVicioByIdService(RegraVicioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RegraVicioDto> findById(BuscarRegravicioPorIdCommand command) {
        return repository.findById(command.regraId()).map(regraVicio -> {
            if (!regraVicio.getVicio().getId().equals(command.vicioId())) {
                throw  new RegraVicioNaoPertenceAoVicioException();
            }

            return RegraVicioDto.fromDomain(regraVicio);
        });
    }
}
