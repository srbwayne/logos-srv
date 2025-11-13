package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

public record UpdateAtividadeConfigCommand(
        AtividadeConfigId atividadeConfigId,
        String nome,
        String descricao,
        Integer xpBase,
        Integer estresseBase,
        Integer diasParaPenalidade,
        Integer xpPerdaPorCiclo
) {}
