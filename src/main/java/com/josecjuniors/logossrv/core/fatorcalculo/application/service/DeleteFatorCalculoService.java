package com.josecjuniors.logossrv.core.fatorcalculo.application.service;

import com.josecjuniors.logossrv.core.fatorcalculo.application.port.in.DeleteFatorCalculoUseCase;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteFatorCalculoService implements DeleteFatorCalculoUseCase {

    private final FatorCalculoRepository repository;

    public DeleteFatorCalculoService(FatorCalculoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(FatorCalculoId id) {
        repository.deleteById(id);
    }
}
