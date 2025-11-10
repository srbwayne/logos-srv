package com.josecjuniors.logossrv.core.jogador.application.dto;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;

public record ResumoJogadorDto(
        String apelido,
        Integer nivelAtual,
        Long xpTotal,
        Long xpParaProximoNivel
) {
    public static ResumoJogadorDto fromDomain(Jogador jogador, NivelXPService nivelXPService) {
        return new ResumoJogadorDto(
                jogador.getApelido(),
                jogador.getNivelAtual(),
                jogador.getXpTotal(),
                nivelXPService.getXpParaProximoNivel(jogador.getNivelAtual())
        );
    }
}
