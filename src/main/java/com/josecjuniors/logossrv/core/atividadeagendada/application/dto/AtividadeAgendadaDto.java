package com.josecjuniors.logossrv.core.atividadeagendada.application.dto;

import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.AtividadeAgendada;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.enums.StatusAtividadeAgendada;

import java.time.LocalDateTime;

public record AtividadeAgendadaDto(
        String id,
        String atividadeNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        StatusAtividadeAgendada status
) {
    public static AtividadeAgendadaDto fromDomain(AtividadeAgendada agendamento) {
        return new AtividadeAgendadaDto(
                agendamento.getId().getValue().toString(),
                agendamento.getAtividadeConfig().getNome(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus()
        );
    }
}
