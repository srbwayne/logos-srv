package com.josecjuniors.logossrv.adapters.in.web.jogador.dto.response;

import com.josecjuniors.logossrv.core.jogador.application.dto.ResumoJogadorDto;

public record ResumoJogadorResponse(
        String apelido,
        Integer nivelAtual,
        Long xpTotal,
        Long xpParaProximoNivel
) {
    public static ResumoJogadorResponse fromDto(ResumoJogadorDto dto) {
        return new ResumoJogadorResponse(
                dto.apelido(),
                dto.nivelAtual(),
                dto.xpTotal(),
                dto.xpParaProximoNivel()
        );
    }
}
