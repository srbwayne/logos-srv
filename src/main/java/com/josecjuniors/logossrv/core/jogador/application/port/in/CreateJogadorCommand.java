package com.josecjuniors.logossrv.core.jogador.application.port.in;

// Usamos um record para um objeto de transferência de dados (DTO) imutável e conciso.
public record CreateJogadorCommand(
        String email,
        String password,
        String nomeExibicao
) {
    public CreateJogadorCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email não pode ser nulo ou vazio.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia.");
        }
    }
}
