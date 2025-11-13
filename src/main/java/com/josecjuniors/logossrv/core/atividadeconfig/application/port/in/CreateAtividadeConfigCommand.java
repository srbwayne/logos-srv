package com.josecjuniors.logossrv.core.atividadeconfig.application.port.in;

public record CreateAtividadeConfigCommand(
        String nome,
        String descricao,
        Integer xpBase,
        Integer estresseBase,
        Integer diasParaPenalidade,
        Integer xpPerdaPorCiclo
) {}
