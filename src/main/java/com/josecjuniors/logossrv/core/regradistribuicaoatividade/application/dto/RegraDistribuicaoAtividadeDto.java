package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto;


public record RegraDistribuicaoAtividadeDto(
        String id,
        String atividadeConfigNome,
        String atributoNome,
        Double pesoPercentual
) {
}
