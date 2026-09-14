package com.josecjuniors.logossrv.core.regrafatorxp.application.dto;


public record RegraFatorXPDto(
        String id,
        String fatorCalculoNome,
        Double pesoMultiplicador,
        Double pontoCorteMin,
        Double pontoCorteMax
) {
}
