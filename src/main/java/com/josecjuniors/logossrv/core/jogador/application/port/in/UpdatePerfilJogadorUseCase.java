package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;

public interface UpdatePerfilJogadorUseCase {
    JogadorDto updatePerfil(UpdatePerfilJogadorCommand command);
}
