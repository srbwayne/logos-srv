package com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.response;

import com.josecjuniors.logossrv.core.atividadeagendada.application.dto.AtividadeAgendadaDto;
import com.josecjuniors.logossrv.core.atividadeagendada.domain.model.enums.StatusAtividadeAgendada;

import java.time.LocalDateTime;

public record AtividadeAgendadaResponse(
        String id,
        String atividadeNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        StatusAtividadeAgendada status
) {
    public static AtividadeAgendadaResponse fromDto(AtividadeAgendadaDto dto) {
        return new AtividadeAgendadaResponse(
                dto.id(),
                dto.atividadeNome(),
                dto.dataHoraInicio(),
                dto.dataHoraFim(),
                dto.status()
        );
    }
}
