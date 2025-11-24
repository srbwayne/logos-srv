package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.command.CreateDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.DebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.CreateDebuffUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.exception.DebuffJaExisteException;
import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.repository.DebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateDebuffService implements CreateDebuffUseCase {

    private final DebuffRepository repository;

    public CreateDebuffService(DebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebuffDto create(CreateDebuffCommand command) {
        if (repository.existsByNome(command.nome())) {
            throw new DebuffJaExisteException();
        }
        var debuff = new Debuff(DebuffId.generate(), command.nome());
        var debuffSalvo = repository.save(debuff);
        return DebuffDto.fromDomain(debuffSalvo);
    }
}
