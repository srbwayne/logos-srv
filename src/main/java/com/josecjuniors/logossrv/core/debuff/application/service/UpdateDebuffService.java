package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.command.UpdateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.UpdateDebuffUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffJaExisteException;
import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffNaoEncontradoException;
import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UpdateDebuffService implements UpdateDebuffUseCase {

    private final DebuffRepository repository;

    public UpdateDebuffService(DebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebuffDto update(UpdateDebuffCommand command) {
        Debuff debuff = repository.findById(command.id())
                .orElseThrow(DebuffNaoEncontradoException::new);


        if (repository.existsByNomeAndIdNot(command.nome(), debuff.getId())) {
            throw new DebuffJaExisteException();
        }

        debuff.atualizar(command.nome());
        Debuff debuffSalvo = repository.save(debuff);
        return DebuffDto.fromDomain(debuffSalvo);
    }
}
