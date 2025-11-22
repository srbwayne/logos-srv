package com.josecjuniors.logossrv.adapters.in.web.habilidade.dto.request;

import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

import java.util.UUID;

public record CreateHabilidadeRequisitoRequest(
    TipoRequisito tipoRequisito,
    UUID requisitoId, // Nulo para o tipo JOGADOR
    Integer nivelMinimo
) {}
