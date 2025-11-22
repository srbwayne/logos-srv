package com.josecjuniors.logossrv.core.jogador.application.dto;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;

public record JogadorResumoDto(
        String apelido,
        Integer nivelAtual,
        Long xpTotal,
        Long xpParaProximoNivel,
        Integer estresseAtual,
        Integer pontosHabilidade
) {
    public static JogadorResumoDto fromDomain(Jogador jogador, NivelXPService nivelXPService) {
        return new JogadorResumoDto(
                jogador.getApelido(),
                jogador.getNivelAtual(),
                jogador.getXpTotal(),
                nivelXPService.calcularXpParaProximoNivel(jogador.getNivelAtual()),
                jogador.getEstresseGlobal().getPontuacaoAtual(),
                jogador.getPontosHabilidade()
        );
    }
}
