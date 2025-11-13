package com.josecjuniors.logossrv.adapters.in.web.regrafatorxp.dto.response;

import com.josecjuniors.logossrv.core.regrafatorxp.application.dto.RegraFatorXPDto;

public record RegraFatorXPResponse(
        String id,
        String fatorCalculoNome,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {
    public static RegraFatorXPResponse fromDto(RegraFatorXPDto dto) {
        return new RegraFatorXPResponse(
                dto.id(),
                dto.fatorCalculoNome(),
                dto.pesoMultiplicador(),
                dto.pontoCorteMin(),
                dto.pontoCorteMax()
        );
    }
}
