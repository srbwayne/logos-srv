package com.josecjuniors.logossrv.core.registrovicio.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.VicioJogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.repository.VicioJogadorRepository;
import com.josecjuniors.logossrv.core.registrovicio.application.command.CreateRegistroVicioCommand;
import com.josecjuniors.logossrv.core.registrovicio.application.dto.RegistroVicioDto;
import com.josecjuniors.logossrv.core.registrovicio.application.port.in.CreateRegistroVicioUseCase;
import com.josecjuniors.logossrv.core.registrovicio.domain.events.RegistroVicioCriadoEvent;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;
import com.josecjuniors.logossrv.core.registrovicio.domain.repository.RegistroVicioRepository;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.repository.VicioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegistroVicioService implements CreateRegistroVicioUseCase {

    private final RegistroVicioRepository registroVicioRepository;
    private final JogadorRepository jogadorRepository;
    private final VicioJogadorRepository vicioJogadorRepository;
    private final VicioRepository vicioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegistroVicioService(RegistroVicioRepository registroVicioRepository, JogadorRepository jogadorRepository, VicioJogadorRepository vicioJogadorRepository, VicioRepository vicioRepository, ApplicationEventPublisher eventPublisher) {
        this.registroVicioRepository = registroVicioRepository;
        this.jogadorRepository = jogadorRepository;
        this.vicioJogadorRepository = vicioJogadorRepository;
        this.vicioRepository = vicioRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RegistroVicioDto create(CreateRegistroVicioCommand command) {

        Jogador jogador = jogadorRepository.findByUserEmail(command.userEmail())
                .orElseThrow(JogadorNaoEncontradoException::new);

        Vicio vicio = vicioRepository.findById(command.vicioId())
                .orElseThrow(VicioJogadorNaoEncontradoException::new);

        var vicioJogador = vicioJogadorRepository
                .findByJogadorIdAndVicioId(jogador.getId(), vicio.getId())
                .orElseGet(() -> new VicioJogador(VicioJogadorId.generate(), jogador, vicio));

        vicioJogadorRepository.save(vicioJogador);


        var novoRegistro = new RegistroVicio(
                RegistroVicioId.generate(),
                vicioJogador,
                command.dataHora(),
                command.observacao()
        );

        var registroSalvo = registroVicioRepository.save(novoRegistro);

        eventPublisher.publishEvent(new RegistroVicioCriadoEvent(registroSalvo.getId()));

        return RegistroVicioDto.fromDomain(registroSalvo);
    }
}
