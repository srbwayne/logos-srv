package com.josecjuniors.logossrv.core.debuff.application.port.in;

import com.josecjuniors.logossrv.core.debuff.application.command.DeleteRegraDistribuicaoDebuffCommand;

public interface DeleteRegraDistribuicaoDebuffUseCase {
    void delete(DeleteRegraDistribuicaoDebuffCommand command);
}
