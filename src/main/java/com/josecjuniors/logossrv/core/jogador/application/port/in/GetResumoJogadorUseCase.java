package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.application.dto.ResumoJogadorDto;

public interface GetResumoJogadorUseCase {
    ResumoJogadorDto getResumo(String jogadorEmail);
}
