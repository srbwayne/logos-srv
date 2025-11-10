package com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response;

import com.josecjuniors.logossrv.core.jogador.application.dto.AtributoJogadorDto;

public record AtributoJogadorResponse(
        String nome,
        String descricao,
        Long xpTotal,
        Long xpParaProximoNivel,
        Integer nivelAtual
) {
    public static AtributoJogadorResponse fromDto(AtributoJogadorDto dto) {
        return new AtributoJogadorResponse(
                dto.nome(),
                dto.descricao(),
                dto.xpTotal(),
                dto.xpParaProximoNivel(),
                dto.nivelAtual()
        );
    }
}
