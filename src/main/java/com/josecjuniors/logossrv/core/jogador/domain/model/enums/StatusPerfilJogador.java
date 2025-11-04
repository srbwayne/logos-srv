package com.josecjuniors.logossrv.core.jogador.domain.model.enums;

public enum StatusPerfilJogador {
    /**
     * Estado inicial, quando o jogador foi criado junto com o AppUser, mas ainda não preencheu seus dados pessoais.
     */
    INCOMPLETO,

    /**
     * O jogador já preencheu seus dados pessoais obrigatórios.
     */
    COMPLETO
}
