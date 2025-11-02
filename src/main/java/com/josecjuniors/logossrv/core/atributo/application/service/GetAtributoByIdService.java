package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.GetAtributoByIdCommand;
import com.josecjuniors.logossrv.core.atributo.application.port.in.GetAtributoByIdUseCase;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetAtributoByIdService implements GetAtributoByIdUseCase {

    private final AtributoRepository atributoRepository;

    public GetAtributoByIdService(AtributoRepository atributoRepository) {
        this.atributoRepository = atributoRepository;
    }

    @Override
    public Optional<AtributoDto> getAtributoById(GetAtributoByIdCommand command) {
        return atributoRepository.findById(command.atributoId())
                .map(this::toDto);
    }

    private AtributoDto toDto(Atributo atributo) {
        return new AtributoDto(
                atributo.getId().getValue().toString(),
                atributo.getNome()
        );
    }
}
