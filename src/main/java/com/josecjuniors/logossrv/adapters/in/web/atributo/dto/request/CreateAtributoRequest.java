package com.josecjuniors.logossrv.adapters.in.web.atributo.dto.request;

public record CreateAtributoRequest(
        String nome,
        String descricao,
        String semanticKey
) {
    public CreateAtributoRequest(String nome, String descricao) {
        this(nome, descricao, null);
    }
}
