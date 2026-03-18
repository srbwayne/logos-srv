package com.josecjuniors.logossrv.core.registrovicio.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.model.DebuffJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.DebuffJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.DebuffJogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.registrovicio.domain.events.RegistroVicioCriadoEvent;
import com.josecjuniors.logossrv.core.registrovicio.domain.exception.RegistroVicioNaoEncontradoException;
import com.josecjuniors.logossrv.core.registrovicio.domain.repository.RegistroVicioRepository;
import com.josecjuniors.logossrv.core.vicio.application.service.NivelVicioService;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Service
public class ProcessarRegistroVicioService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessarRegistroVicioService.class);

    private final RegistroVicioRepository registroVicioRepository;
    private final DebuffJogadorRepository debuffJogadorRepository;
    private final NivelVicioService nivelVicioService;

    public ProcessarRegistroVicioService(RegistroVicioRepository registroVicioRepository, DebuffJogadorRepository debuffJogadorRepository, NivelVicioService nivelVicioService) {
        this.registroVicioRepository = registroVicioRepository;
        this.debuffJogadorRepository = debuffJogadorRepository;
        this.nivelVicioService = nivelVicioService;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processar(RegistroVicioCriadoEvent event) {
        processarEvento(event);
    }

    private void processarEvento(RegistroVicioCriadoEvent event) {
        logger.info("Processando registro de vício ID: {}", event.registroVicioId().getValue());

        var registroVicio = registroVicioRepository.findById(event.registroVicioId())
                .orElseThrow(RegistroVicioNaoEncontradoException::new);

        VicioJogador vicioJogador = registroVicio.getVicioJogador();
        RegraVicio regraVicio = vicioJogador.getVicio().getRegras().stream().findFirst().orElse(null);

        // 1. Reseta a "corrente" de dias sem o vício
        vicioJogador.registrarRecaida();

        if (regraVicio != null) {
            // 2. Fortalece o vício (adiciona XP)
            if (regraVicio.getXpGanhoRecaida() != null) {
                nivelVicioService.adicionarExperiencia(vicioJogador, regraVicio.getXpGanhoRecaida());
            }

            // 3. Aplica estresse imediato
            if (regraVicio.getImpactoEstresse() != null) {
                vicioJogador.getJogador().aplicarEstresse(regraVicio.getImpactoEstresse());
            }

            // 4. Aplica o Debuff
            if (regraVicio.getDebuff() != null && regraVicio.getDuracaoHoras() != null) {
                aplicarDebuff(vicioJogador, regraVicio);
            }
        }

        logger.info("Registro de vício ID: {} processado com sucesso.", event.registroVicioId().getValue());
    }

    private void aplicarDebuff(VicioJogador vicioJogador, RegraVicio regraVicio) {
        var debuff = regraVicio.getDebuff();
        var jogador = vicioJogador.getJogador();
        int potencia = regraVicio.getPenalidadePontos() != null ? regraVicio.getPenalidadePontos() : 0;
        LocalDateTime novaDataExpiracao = LocalDateTime.now().plusHours(regraVicio.getDuracaoHoras());

        DebuffJogador debuffJogador = debuffJogadorRepository
                .findByJogadorIdAndDebuffId(jogador.getId(), debuff.getId())
                .orElse(null);

        if (debuffJogador != null) {
            debuffJogador.acumular(potencia, novaDataExpiracao);
        } else {
            debuffJogador = new DebuffJogador(DebuffJogadorId.generate(), jogador, debuff, potencia, novaDataExpiracao);
        }
        debuffJogadorRepository.save(debuffJogador);
    }
}
