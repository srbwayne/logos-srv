package com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.port.in;

import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.application.command.DeletarRegradistribuicaoHabilidadeCommand;

public interface DeleteRegraDistribuicaoHabilidadeUseCase {
    void delete(DeletarRegradistribuicaoHabilidadeCommand command);
}
