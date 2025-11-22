package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;

import java.util.List;

public interface GetAllRegrasByHabilidadeUseCase {
    List<RegraDistribuicaoHabilidadeDto> getAllByHabilidade(HabilidadeId habilidadeId);
}
