package com.josecjuniors.logossrv.core.atividadeagendada.application.service;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.CreateAtividadeAgendadaCommand;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.CreateAtividadeAgendadaUseCase;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendadaId;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.exception.AtividadeConfigNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateAtividadeAgendadaService implements CreateAtividadeAgendadaUseCase {

    private final AtividadeAgendadaRepository agendamentoRepository;
    private final JogadorRepository jogadorRepository;
    private final AtividadeConfigRepository atividadeConfigRepository;

    public CreateAtividadeAgendadaService(AtividadeAgendadaRepository agendamentoRepository, JogadorRepository jogadorRepository, AtividadeConfigRepository atividadeConfigRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.jogadorRepository = jogadorRepository;
        this.atividadeConfigRepository = atividadeConfigRepository;
    }

    @Override
    public AtividadeAgendadaDto create(CreateAtividadeAgendadaCommand command) {
        Jogador jogador = jogadorRepository.findById(command.jogadorId())
                .orElseThrow(JogadorNaoEncontradoException::new);
        AtividadeConfig atividadeConfig = atividadeConfigRepository.findById(command.atividadeConfigId())
                .orElseThrow(AtividadeConfigNaoEncontradaException::new);

        AtividadeAgendada novoAgendamento = new AtividadeAgendada(
                AtividadeAgendadaId.generate(),
                jogador,
                atividadeConfig,
                command.dataHoraInicio(),
                command.dataHoraFim()
        );

        AtividadeAgendada agendamentoSalvo = agendamentoRepository.save(novoAgendamento);
        return AtividadeAgendadaDto.fromDomain(agendamentoSalvo);
    }
}
