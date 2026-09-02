package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.ProcessarRegistroAtividadeUseCase;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ProcessarRegistroAtividadeService implements ProcessarRegistroAtividadeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessarRegistroAtividadeService.class);
    private final RegistroAtividadeRepository registroAtividadeRepository;
    private final JogadorRepository jogadorRepository;
    private final NivelXPService nivelXPService;
    private final ProgressionEngine progressionEngine;

    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository, NivelXPService nivelXPService) {
        this(registroAtividadeRepository, jogadorRepository, nivelXPService, new ProgressionEngine());
    }

    @Autowired
    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository, NivelXPService nivelXPService, ProgressionEngine progressionEngine) {
        this.registroAtividadeRepository = registroAtividadeRepository;
        this.jogadorRepository = jogadorRepository;
        this.nivelXPService = nivelXPService;
        this.progressionEngine = progressionEngine;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void processar(RegistroAtividadeCriadoEvent event) {
        processarEvento(event);
    }

    public void processarEvento(RegistroAtividadeCriadoEvent event) {
        logger.info("Processando registro de atividade ID: {}", event.registroAtividadeId().getValue());

        RegistroAtividade registro = registroAtividadeRepository.findByIdWithDetails(event.registroAtividadeId())
                .orElseThrow(() -> new IllegalStateException("Registro de Atividade não encontrado para processamento. ID: " + event.registroAtividadeId().getValue()));

        ProgressionResult result = progressionEngine.calculate(registro);

        Jogador jogador = registro.getJogador();
        jogador.aplicarEstresse((int) result.stressTotal());

        result.attributeProgressions().forEach(attributeProgression -> {
            AtributoJogador atributoJogador = jogador.adicionarAtributo(attributeProgression.atributo());
            nivelXPService.adicionarExperiencia(atributoJogador, attributeProgression.xp());
        });

        nivelXPService.adicionarExperiencia(jogador, result.xpGlobal());
        jogadorRepository.save(jogador);

        registro.marcarComoProcessado((int) result.xpGlobal(), (int) result.stressTotal());
        registroAtividadeRepository.save(registro);

        logger.info("Registro de atividade ID: {} processado com sucesso.", event.registroAtividadeId().getValue());
    }

}
