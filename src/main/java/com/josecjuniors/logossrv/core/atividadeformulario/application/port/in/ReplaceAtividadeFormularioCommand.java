package com.josecjuniors.logossrv.core.atividadeformulario.application.port.in;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

import java.util.List;
import java.util.UUID;

public record ReplaceAtividadeFormularioCommand(
        AtividadeConfigId atividadeConfigId,
        long expectedVersion,
        List<Campo> campos) {

    public record Campo(UUID fatorCalculoId, String placeholder) {}
}
