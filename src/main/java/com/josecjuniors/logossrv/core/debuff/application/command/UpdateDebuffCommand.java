package com.josecjuniors.logossrv.core.debuff.application.command;

import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;

public record UpdateDebuffCommand(DebuffId id, String nome) {}
