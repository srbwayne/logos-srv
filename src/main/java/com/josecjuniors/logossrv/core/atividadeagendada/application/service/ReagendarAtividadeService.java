package com.josecjuniors.logossrv.core.atividadeagendada.application.service;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.ReagendarAtividadeCommand;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.ReagendarAtividadeUseCase;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.exception.AtividadeAgendadaNaoEncontradaException;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReagendarAtividadeService implements ReagendarAtividadeUseCase {

    private final AtividadeAgendadaRepository repository;

    public ReagendarAtividadeService(AtividadeAgendadaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AtividadeAgendadaDto reagendar(ReagendarAtividadeCommand command) {
        AtividadeAgendada agendamento = repository.findById(command.atividadeAgendadaId())
                .orElseThrow(AtividadeAgendadaNaoEncontradaException::new);

        // Validação de posse: Garante que o agendamento pertence ao jogador correto.
        if (!agendamento.getJogador().getId().equals(command.jogadorId())) {
            throw new SecurityException("Este agendamento não pertence ao jogador informado.");
        }

        agendamento.reagendar(command.novaDataHoraInicio(), command.novaDataHoraFim());

        AtividadeAgendada agendamentoSalvo = repository.save(agendamento);
        return AtividadeAgendadaDto.fromDomain(agendamentoSalvo);
    }
}
