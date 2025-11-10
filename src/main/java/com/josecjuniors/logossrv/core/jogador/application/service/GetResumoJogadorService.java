package com.josecjuniors.logossrv.core.jogador.application.service;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.application.dto.ResumoJogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.GetResumoJogadorUseCase;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetResumoJogadorService implements GetResumoJogadorUseCase {

    private final JogadorRepository jogadorRepository;
    private final NivelXPService nivelXPService;

    public GetResumoJogadorService(JogadorRepository jogadorRepository, NivelXPService nivelXPService) {
        this.jogadorRepository = jogadorRepository;
        this.nivelXPService = nivelXPService;
    }

    @Override
    public ResumoJogadorDto getResumo(String jogadorEmail) {
        Jogador jogador = jogadorRepository.findByUserEmail(jogadorEmail)
                .orElseThrow(JogadorNaoEncontradoException::new);

        return ResumoJogadorDto.fromDomain(jogador, nivelXPService);
    }
}
