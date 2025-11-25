package com.josecjuniors.logossrv.core.vicio.application.command;

import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;

public record UpdateVicioCommand(VicioId id, String nome, String descricao) {}
