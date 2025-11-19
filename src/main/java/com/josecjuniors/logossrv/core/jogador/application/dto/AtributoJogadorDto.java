package com.josecjuniors.logossrv.core.jogador.application.dto;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;

public record AtributoJogadorDto(
        String nome,
        String descricao,
        Long xpTotal,
        Long xpParaProximoNivel,
        Integer nivelAtual
) {
    public static AtributoJogadorDto fromDomain(AtributoJogador domain, NivelXPService nivelXPService) {
        return new AtributoJogadorDto(
                domain.getAtributo().getNome(),
                domain.getAtributo().getDescricao(),
                domain.getXpTotal(),
                nivelXPService.calcularXpParaProximoNivel(domain.getNivelAtual()),
                domain.getNivelAtual()
        );
    }
}
