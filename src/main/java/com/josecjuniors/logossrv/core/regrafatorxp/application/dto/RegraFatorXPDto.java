package com.josecjuniors.logossrv.core.regrafatorxp.application.dto;

import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;

public record RegraFatorXPDto(
        String id,
        String fatorCalculoNome,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {
    public static RegraFatorXPDto fromDomain(RegraFatorXP regra) {
        return new RegraFatorXPDto(
                regra.getId().getValue().toString(),
                regra.getFatorCalculo().getNome(),
                regra.getPesoMultiplicador(),
                regra.getPontoCorteMin(),
                regra.getPontoCorteMax()
        );
    }
}
