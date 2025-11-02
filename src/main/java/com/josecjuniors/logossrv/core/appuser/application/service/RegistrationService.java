package com.josecjuniors.logossrv.core.appuser.application.service;

import com.josecjuniors.logossrv.core.appuser.application.dto.RegistrationResult;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationCommand;
import com.josecjuniors.logossrv.core.appuser.application.port.in.RegistrationUseCase;
import com.josecjuniors.logossrv.core.appuser.domain.exception.EmailJaCadastradoException;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.appuser.domain.repository.AppUserRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrationService implements RegistrationUseCase {

    private final AppUserRepository appUserRepository;
    private final JogadorRepository jogadorRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AppUserRepository appUserRepository, JogadorRepository jogadorRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.jogadorRepository = jogadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegistrationResult register(RegistrationCommand command) {
        if (appUserRepository.existsByEmail(command.email())) {
            throw new EmailJaCadastradoException();
        }

        AppUser newUser = new AppUser(
                new AppUserId(),
                command.email(),
                passwordEncoder.encode(command.password())
        );
        AppUser savedUser = appUserRepository.save(newUser);

        Jogador novoJogador = new Jogador(
                new JogadorId(),
                savedUser,
                command.nomeExibicao()
        );
        Jogador savedJogador = jogadorRepository.save(novoJogador);

        return new RegistrationResult(savedUser, savedJogador);
    }
}
