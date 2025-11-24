package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.command.BuscarRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;

import java.util.Optional;

public interface GetRegraDistribuicaoDebuffByIdUseCase {
    Optional<RegraDistribuicaoDebuffDto> findById(BuscarRegraDistribuicaoDebuffCommand command);
}
