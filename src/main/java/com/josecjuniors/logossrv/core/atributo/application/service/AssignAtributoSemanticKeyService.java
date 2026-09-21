package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.AssignAtributoSemanticKeyUseCase;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoSemanticKeyJaExisteException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoSemanticKey;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssignAtributoSemanticKeyService implements AssignAtributoSemanticKeyUseCase {
    private final AtributoRepository repository;

    public AssignAtributoSemanticKeyService(AtributoRepository repository) {
        this.repository = repository;
    }

    @Override
    public AtributoDto assign(AtributoId id, String semanticKey) {
        Atributo atributo = repository.findById(id).orElseThrow(AtributoNaoEncontradoException::new);
        String normalized = AtributoSemanticKey.of(semanticKey).value();
        if (repository.existsBySemanticKeyAndIdNot(normalized, id)) {
            throw new AtributoSemanticKeyJaExisteException(normalized);
        }
        atributo.assignSemanticKey(normalized);
        Atributo saved = repository.save(atributo);
        return toDto(saved);
    }

    private AtributoDto toDto(Atributo atributo) {
        return new AtributoDto(atributo.getId().getValue().toString(), atributo.getNome(), atributo.getDescricao(), atributo.getSemanticKey());
    }
}
