package com.josecjuniors.logossrv.core.debuff.application.service;

import com.josecjuniors.logossrv.core.debuff.application.command.BuscarRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.application.port.in.GetRegraDistribuicaoDebuffByIdUseCase;
import com.josecjuniors.logossrv.core.debuff.domain.exception.RegraDistribuicaoDebuffNaoPertenceADebuffException;
import com.josecjuniors.logossrv.core.debuff.domain.repository.RegraDistribuicaoDebuffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetRegraDistribuicaoDebuffByIdService implements GetRegraDistribuicaoDebuffByIdUseCase {

    private final RegraDistribuicaoDebuffRepository repository;

    public GetRegraDistribuicaoDebuffByIdService(RegraDistribuicaoDebuffRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RegraDistribuicaoDebuffDto> findById(BuscarRegraDistribuicaoDebuffCommand command) {

        return repository.findById(command.regraId())
                .map(regra -> {
                    if (!regra.getDebuff().getId().equals(command.debuffId())) {
                        throw new RegraDistribuicaoDebuffNaoPertenceADebuffException();
                    }
                    return RegraDistribuicaoDebuffDto.fromDomain(regra);
                });

    }
}
