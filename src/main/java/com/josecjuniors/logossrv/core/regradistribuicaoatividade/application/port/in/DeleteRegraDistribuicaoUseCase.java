package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;

public interface DeleteRegraDistribuicaoUseCase {
    void delete(RegraDistribuicaoAtividadeId id);
}
