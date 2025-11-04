package com.josecjuniors.logossrv.core.jogador.application.port.in;

public record UpdateApelidoCommand(
        String jogadorEmail, // Para identificar o jogador
        String novoApelido
) {}
