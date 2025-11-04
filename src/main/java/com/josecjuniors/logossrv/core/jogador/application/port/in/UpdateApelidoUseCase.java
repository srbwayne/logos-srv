package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;

public interface UpdateApelidoUseCase {
    JogadorDto updateApelido(UpdateApelidoCommand command);
}
