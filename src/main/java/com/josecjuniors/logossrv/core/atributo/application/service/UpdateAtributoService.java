package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.UpdateAtributoCommand;
import com.josecjuniors.logossrv.core.atributo.application.port.in.UpdateAtributoUseCase;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoJaExisteException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateAtributoService implements UpdateAtributoUseCase {

    private final AtributoRepository atributoRepository;

    public UpdateAtributoService(AtributoRepository atributoRepository) {
        this.atributoRepository = atributoRepository;
    }

    @Override
    public AtributoDto updateAtributo(UpdateAtributoCommand command) {
        Atributo atributo = atributoRepository.findById(command.atributoId())
                .orElseThrow(AtributoNaoEncontradoException::new);

        if (atributoRepository.existsByNomeAndIdNot(command.novoNome(), command.atributoId())) {
            throw new AtributoJaExisteException(command.novoNome());
        }

        atributo.atualizarNome(command.novoNome());
        atributo.atualizarDescricao(command.novaDescricao());
        Atributo atributoAtualizado = atributoRepository.save(atributo);

        return toDto(atributoAtualizado);
    }

    private AtributoDto toDto(Atributo atributo) {
        return new AtributoDto(
                atributo.getId().getValue().toString(),
                atributo.getNome(),
                atributo.getDescricao()
        );
    }
}
