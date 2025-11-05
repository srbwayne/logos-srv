package com.josecjuniors.logossrv.core.atributo.application.port.in;

public record CreateAtributoCommand(
        String nome,
        String descricao
) {}
