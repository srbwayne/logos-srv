package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.command.DeleteRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.port.in.DeleteRegraDistribuicaoDebuffUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.exception.RegraDistribuicaoDebuffNaoEncontradaException;
import com.josecjuniors.logossrv.core.debuff.domain.exception.RegraDistribuicaoDebuffNaoPertenceADebuffException;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteRegraDistribuicaoDebuffService implements DeleteRegraDistribuicaoDebuffUseCase {

    private final RegraDistribuicaoDebuffRepository repository;

    public DeleteRegraDistribuicaoDebuffService(RegraDistribuicaoDebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(DeleteRegraDistribuicaoDebuffCommand command) {
        RegraDistribuicaoDebuff regra = repository.findById(command.regraId())
                .orElseThrow(RegraDistribuicaoDebuffNaoEncontradaException::new);

        if (!regra.getDebuff().getId().equals(command.debuffId())) {
            throw new RegraDistribuicaoDebuffNaoPertenceADebuffException();
        }

        repository.delete(regra);
    }
}
