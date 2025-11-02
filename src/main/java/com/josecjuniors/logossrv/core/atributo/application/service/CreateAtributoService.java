package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.CreateAtributoCommand;
import com.josecjuniors.logossrv.core.atributo.application.port.in.CreateAtributoUseCase;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoJaExisteException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateAtributoService implements CreateAtributoUseCase {

    private final AtributoRepository atributoRepository;

    public CreateAtributoService(AtributoRepository atributoRepository) {
        this.atributoRepository = atributoRepository;
    }

    @Override
    public AtributoDto createAtributo(CreateAtributoCommand command) {
        if (atributoRepository.existsByNome(command.nome())) {
            throw new AtributoJaExisteException(command.nome());
        }

        Atributo novoAtributo = new Atributo(new AtributoId(), command.nome());
        Atributo atributoSalvo = atributoRepository.save(novoAtributo);

        return toDto(atributoSalvo);
    }

    private AtributoDto toDto(Atributo atributo) {
        return new AtributoDto(
                atributo.getId().getValue().toString(),
                atributo.getNome()
        );
    }
}
