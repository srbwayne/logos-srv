package com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.response;

import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

public record HabilidadeRequisitoResponse(
        String id,
        TipoRequisito tipo,
        String nomeRequisito,
        Integer nivelMinimo
) {
    public static HabilidadeRequisitoResponse fromDto(HabilidadeRequisitoDto dto) {
        return new HabilidadeRequisitoResponse(
                dto.id(),
                dto.tipo(),
                dto.nomeRequisito(),
                dto.nivelMinimo()
        );
    }
}
