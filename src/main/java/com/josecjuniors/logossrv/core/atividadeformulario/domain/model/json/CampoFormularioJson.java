package com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json;

import java.util.UUID;

/**
 * Representa um único campo de input no formulário dinâmico.
 */
public record CampoFormularioJson(
    UUID fatorCalculoId,
    String nome,
    String unidadeMedida,
    TipoInput tipoInput,
    String placeholder,
    boolean obrigatorio
) {}
