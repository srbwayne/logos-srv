package com.josecjuniors.logossrv.core.atributo.application.port.in;

public record CreateAtributoCommand(
        String nome,
        String descricao,
        String semanticKey
) {
    public CreateAtributoCommand(String nome, String descricao) {
        this(nome, descricao, null);
    }
}
