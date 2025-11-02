package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;

public interface CreateJogadorUseCase {

    Jogador createJogador(CreateJogadorCommand command);

}
