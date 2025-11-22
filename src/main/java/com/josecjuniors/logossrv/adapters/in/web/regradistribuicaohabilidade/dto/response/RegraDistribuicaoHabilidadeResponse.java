package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.response;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;

import java.util.UUID;

public record RegraDistribuicaoHabilidadeResponse(
        UUID id,
        String habilidadeNome,
        String atributoNome,
        Double pesoDistribuicao
) {
    public static RegraDistribuicaoHabilidadeResponse fromDto(RegraDistribuicaoHabilidadeDto dto) {
        return new RegraDistribuicaoHabilidadeResponse(
                dto.id(),
                dto.habilidadeNome(),
                dto.atributoNome(),
                dto.pesoDistribuicao()
        );
    }
}
