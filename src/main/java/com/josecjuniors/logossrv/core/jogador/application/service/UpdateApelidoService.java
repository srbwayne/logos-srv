package com.josecjuniors.logossrv.core.jogador.application.service;

import com.josecjuniors.logossrv.core.jogador.application.dto.JogadorDto;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoUseCase;
import com.josecjuniors.logossrv.core.jogador.domain.exception.ApelidoJaEmUsoException;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateApelidoService implements UpdateApelidoUseCase {

    private final JogadorRepository jogadorRepository;

    public UpdateApelidoService(JogadorRepository jogadorRepository) {
        this.jogadorRepository = jogadorRepository;
    }

    @Override
    public JogadorDto updateApelido(UpdateApelidoCommand command) {
        Jogador jogador = jogadorRepository.findByUserEmail(command.jogadorEmail())
                .orElseThrow(JogadorNaoEncontradoException::new);

        if (jogadorRepository.existsByApelidoAndIdNot(command.novoApelido(), jogador.getId())) {
            throw new ApelidoJaEmUsoException();
        }

        jogador.atualizarApelido(command.novoApelido());
        
        Jogador jogadorAtualizado = jogadorRepository.save(jogador);

        return JogadorDto.fromDomain(jogadorAtualizado);
    }
}
