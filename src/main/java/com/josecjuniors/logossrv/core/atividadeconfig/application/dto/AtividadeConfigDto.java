package com.josecjuniors.logossrv.core.atividadeconfig.application.dto;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;

public record AtividadeConfigDto(
        String id,
        String nome,
        String descricao,
        Integer xpBase,
        Integer estresseBase,
        Integer diasParaPenalidade,
        Integer xpPerdaPorCiclo
) {
    public static AtividadeConfigDto fromDomain(AtividadeConfig atividade) {
        return new AtividadeConfigDto(
                atividade.getId().getValue().toString(),
                atividade.getNome(),
                atividade.getDescricao(),
                atividade.getXpBase(),
                atividade.getEstresseBase(),
                atividade.getDiasParaPenalidade(),
                atividade.getXpPerdaPorCiclo()
        );
    }
}
