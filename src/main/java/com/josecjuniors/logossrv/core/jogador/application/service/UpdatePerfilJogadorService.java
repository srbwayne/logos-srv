package com.josecjuniors.logossrv.core.jogador.application.service;

import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorUseCase;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdatePerfilJogadorService implements UpdatePerfilJogadorUseCase {

    private final JogadorRepository jogadorRepository;

    public UpdatePerfilJogadorService(JogadorRepository jogadorRepository) {
        this.jogadorRepository = jogadorRepository;
    }

    @Override
    public JogadorDto updatePerfil(UpdatePerfilJogadorCommand command) {
        Jogador jogador = jogadorRepository.findByUserEmail(command.jogadorEmail())
                .orElseThrow(JogadorNaoEncontradoException::new);

        jogador.atualizarPerfil(command);
        
        Jogador jogadorAtualizado = jogadorRepository.save(jogador);

        return JogadorDto.fromDomain(jogadorAtualizado);
    }
}
