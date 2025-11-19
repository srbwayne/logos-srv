package com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json;

import java.util.List;
import java.util.UUID;

/**
 * Enum que define os tipos de input para o formulário dinâmico,
 * carregando também o tipo de dado Java correspondente para validação.
 */
public enum TipoInput {
    NUMERICO(Double.class),
    TEXTO_CURTO(String.class),
    TEXTO_LONGO(String.class),
    SELECAO_UNICA(UUID.class),      // O valor esperado é o ID da opção selecionada
    SELECAO_MULTIPLA(List.class); // O valor esperado é uma lista de IDs

    private final Class<?> tipoJava;

    TipoInput(Class<?> tipoJava) {
        this.tipoJava = tipoJava;
    }

    /**
     * Retorna a classe Java que representa o valor esperado para este tipo de input.
     * @return A classe do tipo de dado.
     */
    public Class<?> getTipoJava() {
        return tipoJava;
    }

    public boolean ehValorNumerico() {
        return this == NUMERICO;
    }
}
