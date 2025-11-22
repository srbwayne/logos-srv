package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;

import java.util.UUID;

public record RegraDistribuicaoHabilidadeDto(
        UUID id,
        UUID habilidadeId,
        String habilidadeNome,
        UUID atributoId,
        String atributoNome,
        Double pesoDistribuicao
) {
    public static RegraDistribuicaoHabilidadeDto fromDomain(RegraDistribuicaoHabilidade domain) {
        return new RegraDistribuicaoHabilidadeDto(
                domain.getId().getValue(),
                domain.getHabilidade().getId().getValue(),
                domain.getHabilidade().getNome(),
                domain.getAtributo().getId().getValue(),
                domain.getAtributo().getNome(),
                domain.getPesoDistribuicao()
        );
    }
}
