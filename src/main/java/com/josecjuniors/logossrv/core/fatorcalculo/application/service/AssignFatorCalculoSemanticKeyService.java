package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.dto.FatorCalculoDto;
import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.AssignFatorCalculoSemanticKeyUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoJaExisteException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.exception.FatorCalculoNaoEncontradoException;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.SemanticKey;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssignFatorCalculoSemanticKeyService implements AssignFatorCalculoSemanticKeyUseCase {
    private final FatorCalculoRepository repository;

    public AssignFatorCalculoSemanticKeyService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public FatorCalculoDto assign(FatorCalculoId id, String semanticKey) {
        var fator = repository.findById(id).orElseThrow(FatorCalculoNaoEncontradoException::new);
        var normalized = SemanticKey.of(semanticKey).value();
        if (repository.existsBySemanticKeyAndIdNot(normalized, id)) {
            throw new FatorCalculoJaExisteException(normalized);
        }
        fator.assignSemanticKey(normalized);
        return FatorCalculoDto.fromDomain(repository.save(fator));
    }
}
