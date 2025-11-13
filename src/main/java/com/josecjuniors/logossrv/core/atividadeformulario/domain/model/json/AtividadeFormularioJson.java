package com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json;

import java.util.List;
import java.util.UUID;

/**
 * DTO que representa a estrutura completa do JSON a ser salvo no banco.
 */
public record AtividadeFormularioJson(
    UUID atividadeConfigId,
    String nomeAtividade,
    String descricaoAtividade,
    List<CampoFormularioJson> campos
) {}
