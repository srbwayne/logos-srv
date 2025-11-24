package com.josecjuniors.logossrv.core.debuff.application.command;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuffId;

public record BuscarRegraDistribuicaoDebuffCommand(
        DebuffId debuffId,
        RegraDistribuicaoDebuffId regraId
) {}
