package com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response;

import com.josecjuniors.logossrv.core.habilidade.application.dto.GetHabilidadeRequisitoByIdDto;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

import java.util.UUID;

public record GetHabilidadeRequisitoByIdResponse(
        String id,
        TipoRequisito tipo,
        UUID requisitoId,
        String requisitoNome,
        Integer nivelMinimo
) {
    public static GetHabilidadeRequisitoByIdResponse fromDto(GetHabilidadeRequisitoByIdDto dto) {
        return new GetHabilidadeRequisitoByIdResponse(
                dto.id(),
                dto.tipo(),
                dto.requisitoId(),
                dto.requisitoNome(),
                dto.nivelMinimo()
        );
    }
}
