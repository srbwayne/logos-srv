package com.josecjuniors.logossrv.core.habilidade.application.port.in.commands;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisitoId;

public record DeletarHabilidadeRequisitoCommand(HabilidadeId habilidadeId, HabilidadeRequisitoId habilidadeRequisitoId) {
}
