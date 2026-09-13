package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.response;

import com.josecjuniors.logossrv.core.atividadeconfig.application.dto.AtividadeConfigDto;

public record AtividadeConfigResponse(
        String id,
        String nome,
        String descricao
) {
    public static AtividadeConfigResponse fromDto(AtividadeConfigDto dto) {
        return new AtividadeConfigResponse(
                dto.id(),
                dto.nome(),
                dto.descricao()
        );
    }
}
