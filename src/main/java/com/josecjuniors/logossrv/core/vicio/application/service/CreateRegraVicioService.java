package com.josecjuniors.logossrv.core.vicio.application.service;

import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffNaoEncontradoException;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import com.josecjuniors.logossrv.core.vicio.application.command.CreateRegraVicioCommand;
import com.josecjuniors.logossrv.core.vicio.application.dto.RegraVicioDto;
import com.josecjuniors.logossrv.core.vicio.application.port.in.CreateRegraVicioUseCase;
import com.josecjuniors.logossrv.core.vicio.domain.exception.VicioNaoEncontradoException;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.repository.RegraVicioRepository;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegraVicioService implements CreateRegraVicioUseCase {

    private final RegraVicioRepository repository;
    private final VicioRepository vicioRepository;
    private final DebuffRepository debuffRepository;

    public CreateRegraVicioService(RegraVicioRepository repository, VicioRepository vicioRepository, DebuffRepository debuffRepository) {
        this.repository = repository;
        this.vicioRepository = vicioRepository;
        this.debuffRepository = debuffRepository;
    }

    @Override
    public RegraVicioDto create(CreateRegraVicioCommand command) {
        var vicio = vicioRepository.findById(command.vicioId())
                .orElseThrow(VicioNaoEncontradoException::new);

        var debuff = command.debuffId() != null ? debuffRepository.findById(command.debuffId())
                .orElseThrow(DebuffNaoEncontradoException::new) : null;

        var novaRegra = new RegraVicio(
                RegraVicioId.generate(),
                vicio,
                command.impactoEstresse(),
                command.penalidadePontos(),
                command.duracaoHoras(),
                command.xpGanhoRecaida(),
                debuff
        );

        var regraSalva = repository.save(novaRegra);
        return RegraVicioDto.fromDomain(regraSalva);
    }
}
