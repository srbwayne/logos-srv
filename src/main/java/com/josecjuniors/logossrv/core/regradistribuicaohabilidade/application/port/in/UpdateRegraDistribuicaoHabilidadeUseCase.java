package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.UpdateRegraDistribuicaoHabilidadeCommand;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.dto.RegraDistribuicaoHabilidadeDto;

public interface UpdateRegraDistribuicaoHabilidadeUseCase {
    RegraDistribuicaoHabilidadeDto update(UpdateRegraDistribuicaoHabilidadeCommand command);
}
