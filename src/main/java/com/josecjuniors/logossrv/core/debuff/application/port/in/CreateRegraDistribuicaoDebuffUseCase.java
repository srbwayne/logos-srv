package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.command.CreateRegraDistribuicaoDebuffCommand;
import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;

public interface CreateRegraDistribuicaoDebuffUseCase {
    RegraDistribuicaoDebuffDto create(CreateRegraDistribuicaoDebuffCommand command);
}
