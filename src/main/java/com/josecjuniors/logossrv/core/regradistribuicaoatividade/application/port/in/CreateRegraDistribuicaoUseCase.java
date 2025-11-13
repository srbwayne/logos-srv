package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;

public interface CreateRegraDistribuicaoUseCase {
    RegraDistribuicaoAtividadeDto create(CreateRegraDistribuicaoCommand command);
}
