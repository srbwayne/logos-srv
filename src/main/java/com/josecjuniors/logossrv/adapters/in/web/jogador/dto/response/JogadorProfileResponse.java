package com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response;

import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.StatusPerfilJogador;

public record JogadorProfileResponse(
        String id,
        String apelido,
        String email,
        StatusPerfilJogador statusPerfil
) {
    public static JogadorProfileResponse fromDto(JogadorDto dto) {
        return new JogadorProfileResponse(dto.id(), dto.apelido(), dto.email(), dto.statusPerfil());
    }
}
