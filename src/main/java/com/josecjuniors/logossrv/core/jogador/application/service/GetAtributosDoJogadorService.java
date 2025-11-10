package com.josecjuniors.logossrv.core.jogador.application.service;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.application.dto.AtributoJogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.GetAtributosDoJogadorUseCase;
import com.josecjuniors.logossrv.core.jogador.domain.repository.AtributoJogadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAtributosDoJogadorService implements GetAtributosDoJogadorUseCase {

    private final AtributoJogadorRepository atributoJogadorRepository;
    private final NivelXPService nivelXPService;

    public GetAtributosDoJogadorService(AtributoJogadorRepository atributoJogadorRepository, NivelXPService nivelXPService) {
        this.atributoJogadorRepository = atributoJogadorRepository;
        this.nivelXPService = nivelXPService;
    }

    @Override
    public Page<AtributoJogadorDto> getAtributos(String jogadorEmail, Pageable pageable) {
        return atributoJogadorRepository.findByJogadorUserEmail(jogadorEmail, pageable)
                .map(atributoJogador -> AtributoJogadorDto.fromDomain(atributoJogador, nivelXPService));
    }
}
