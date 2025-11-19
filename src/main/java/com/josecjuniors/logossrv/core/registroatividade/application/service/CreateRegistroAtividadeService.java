package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeUseCase;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateRegistroAtividadeService implements CreateRegistroAtividadeUseCase {

    private final RegistroAtividadeRepository registroRepository;
    private final JogadorRepository jogadorRepository;
    private final AtividadeConfigRepository atividadeConfigRepository;
    private final FatorCalculoRepository fatorCalculoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRegistroAtividadeService(RegistroAtividadeRepository registroRepository, JogadorRepository jogadorRepository, AtividadeConfigRepository atividadeConfigRepository, FatorCalculoRepository fatorCalculoRepository, ApplicationEventPublisher eventPublisher) {
        this.registroRepository = registroRepository;
        this.jogadorRepository = jogadorRepository;
        this.atividadeConfigRepository = atividadeConfigRepository;
        this.fatorCalculoRepository = fatorCalculoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void create(CreateRegistroAtividadeCommand command) {
        Jogador jogador = jogadorRepository.findByUserEmail(command.userEmail())
                .orElseThrow(JogadorNaoEncontradoException::new);
        AtividadeConfig atividadeConfig = atividadeConfigRepository.findById(new AtividadeConfigId(command.atividadeConfigId()))
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);

        RegistroAtividade novoRegistro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividadeConfig, command.dataHoraInicio(), command.dataHoraFim());

        command.detalhes().forEach(detalhe -> {
            FatorCalculo fator = fatorCalculoRepository.findById(new FatorCalculoId(detalhe.fatorCalculoId()))
                    .orElseThrow(() -> new FatorCalculoNaoEncontradoException(detalhe.valor()));
            novoRegistro.adicionarDetalhe(fator, detalhe.valor());
        });

        RegistroAtividade registroSalvo = registroRepository.save(novoRegistro);

        eventPublisher.publishEvent(new RegistroAtividadeCriadoEvent(registroSalvo.getId()));
    }
}
