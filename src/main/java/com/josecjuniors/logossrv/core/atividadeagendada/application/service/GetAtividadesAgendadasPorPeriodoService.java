package com.josecjuniors.logossrv.core.atividadeagendada.application.service;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.atividadeagendada.application.port.in.GetAtividadesAgendadasPorPeriodoUseCase;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.repository.AtividadeAgendadaRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAtividadesAgendadasPorPeriodoService implements GetAtividadesAgendadasPorPeriodoUseCase {

    private final AtividadeAgendadaRepository repository;

    public GetAtividadesAgendadasPorPeriodoService(AtividadeAgendadaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AtividadeAgendadaDto> getByPeriodo(JogadorId jogadorId, LocalDateTime inicio, LocalDateTime fim) {
        return repository.findByJogadorIdAndPeriodo(jogadorId, inicio, fim).stream()
                .map(AtividadeAgendadaDto::fromDomain)
                .collect(Collectors.toList());
    }
}
