package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.CreateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;

public interface CreateRegraDistribuicaoHabilidadeUseCase {
    RegraDistribuicaoHabilidadeDto create(CreateRegraDistribuicaoHabilidadeCommand command);
}
