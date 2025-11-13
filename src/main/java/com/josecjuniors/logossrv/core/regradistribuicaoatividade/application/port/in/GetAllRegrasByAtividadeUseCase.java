package com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.application.dto.RegraDistribuicaoAtividadeDto;

import java.util.List;

public interface GetAllRegrasByAtividadeUseCase {
    List<RegraDistribuicaoAtividadeDto> getAllByAtividade(AtividadeConfigId atividadeId);
}
