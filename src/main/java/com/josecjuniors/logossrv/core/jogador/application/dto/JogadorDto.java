package com.josecjuniors.logossrv.core.jogador.application.dto;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.StatusPerfilJogador;

public record JogadorDto(
        String id,
        String apelido,
        String email,
        StatusPerfilJogador statusPerfil
) {
    public static JogadorDto fromDomain(Jogador jogador) {
        return new JogadorDto(
                jogador.getId().getValue().toString(),
                jogador.getApelido(),
                jogador.getUser().getEmail(),
                jogador.getStatusPerfil()
        );
    }
}
