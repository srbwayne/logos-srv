package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.application.dto.AtributoJogadorDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAtributosDoJogadorUseCase {
    Page<AtributoJogadorDto> getAtributos(String jogadorEmail, Pageable pageable);
}
