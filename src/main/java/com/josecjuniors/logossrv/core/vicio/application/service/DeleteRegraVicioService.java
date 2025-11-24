package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.vicio.application.command.DeletarRegravicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.port.in.DeleteRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.RegraVicioNaoEncontradaException;
import com.josecjuniors.logossrv.core.vicio.domain.exception.RegraVicioNaoPertenceAoVicioException;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraVicioService implements DeleteRegraVicioUseCase {

    private final RegraVicioRepository regraVicioRepository;

    public DeleteRegraVicioService(RegraVicioRepository regraVicioRepository) {
        this.regraVicioRepository = regraVicioRepository;
    }

    @Override
    public void delete(DeletarRegravicioCommand command) {

        RegraVicio regraVicio = regraVicioRepository.findById(command.regraId())
                        .orElseThrow(RegraVicioNaoEncontradaException::new);

        if (!regraVicio.getVicio().getId().equals(command.vicioId())) {
            throw new RegraVicioNaoPertenceAoVicioException();
        }

        regraVicioRepository.delete(regraVicio);
    }
}
