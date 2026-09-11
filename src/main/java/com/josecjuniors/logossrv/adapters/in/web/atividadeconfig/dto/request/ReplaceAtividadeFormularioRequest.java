package com.josecjuniors.logossrv.adapters.in.web.atividadeconfig.dto.request;

import java.util.List;
import java.util.UUID;

public record ReplaceAtividadeFormularioRequest(
        long expectedVersion,
        List<CampoRequest> campos) {

    public record CampoRequest(UUID fatorCalculoId, String placeholder) {}
}
