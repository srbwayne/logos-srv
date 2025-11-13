package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;

public interface UpdateRegraDistribuicaoUseCase {
    RegraDistribuicaoAtividadeDto update(UpdateRegraDistribuicaoCommand command);
}
