package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaoatividade.dto.response;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;

public record RegraDistribuicaoAtividadeResponse(
        String id,
        String atividadeConfigNome,
        String atributoNome,
        Double pesoPercentual
) {
    public static RegraDistribuicaoAtividadeResponse fromDto(RegraDistribuicaoAtividadeDto dto) {
        return new RegraDistribuicaoAtividadeResponse(
                dto.id(),
                dto.atividadeConfigNome(),
                dto.atributoNome(),
                dto.pesoPercentual()
        );
    }
}
