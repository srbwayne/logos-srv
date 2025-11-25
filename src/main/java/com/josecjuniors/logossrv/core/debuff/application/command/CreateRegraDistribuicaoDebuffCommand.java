package com.josecjuniors.logossrv.core.debuff.application.command;

import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;

public record CreateRegraDistribuicaoDebuffCommand(
        DebuffId debuffId,
        AtributoId atributoId
) {}
