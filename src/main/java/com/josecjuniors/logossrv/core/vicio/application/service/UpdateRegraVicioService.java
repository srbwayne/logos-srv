package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffNaoEncontradoException;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import com.josecjuniors.logossrv.core.vicio.application.command.UpdateRegraVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.UpdateRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.RegraVicioNaoEncontradaException;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateRegraVicioService implements UpdateRegraVicioUseCase {

    private final RegraVicioRepository repository;
    private final DebuffRepository debuffRepository;

    public UpdateRegraVicioService(RegraVicioRepository repository, DebuffRepository debuffRepository) {
        this.repository = repository;
        this.debuffRepository = debuffRepository;
    }

    @Override
    public RegraVicioDto update(UpdateRegraVicioCommand command) {
        RegraVicio regra = repository.findById(command.regraId())
                .orElseThrow(RegraVicioNaoEncontradaException::new);

        if (!regra.getVicio().getId().equals(command.vicioId())) {
            throw new SecurityException("A regra não pertence ao vício informado.");
        }

        var debuff = command.debuffId() != null ? debuffRepository.findById(command.debuffId())
                .orElseThrow(DebuffNaoEncontradoException::new) : null;

        regra.atualizar(
                command.impactoEstresse(),
                command.penalidadePontos(),
                command.duracaoHoras(),
                command.xpGanhoRecaida(),
                debuff
        );

        var regraSalva = repository.save(regra);
        return RegraVicioDto.fromDomain(regraSalva);
    }
}
