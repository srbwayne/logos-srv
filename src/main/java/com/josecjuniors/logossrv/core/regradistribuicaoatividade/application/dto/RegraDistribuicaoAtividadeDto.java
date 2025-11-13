package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;

public record RegraDistribuicaoAtividadeDto(
        String id,
        String atividadeConfigNome,
        String atributoNome,
        Double pesoPercentual
) {
    public static RegraDistribuicaoAtividadeDto fromDomain(RegraDistribuicaoAtividade regra) {
        return new RegraDistribuicaoAtividadeDto(
                regra.getId().getValue().toString(),
                regra.getAtividadeConfig().getNome(),
                regra.getAtributo().getNome(),
                regra.getPesoPercentual()
        );
    }
}
