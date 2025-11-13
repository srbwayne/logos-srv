package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request;

public record CreateAtividadeConfigRequest(
        String nome,
        String descricao,
        Integer xpBase,
        Integer estresseBase,
        Integer diasParaPenalidade,
        Integer xpPerdaPorCiclo
) {}
