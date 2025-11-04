package com.josecjuniors.logossrv.core.jogador.application.port.in;

import com.josecjuniors.logossrv.core.jogador.domain.model.enums.EnderecoEstado;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.Sexo;

import java.time.LocalDate;

public record UpdatePerfilJogadorCommand(
        String jogadorEmail, // Para identificar o jogador a ser atualizado
        String nomeCompleto,
        LocalDate dataNascimento,
        String cpf,
        String numeroTelefone,
        Sexo sexo,
        String descricao,
        String enderecoPais,
        EnderecoEstado enderecoEstado,
        String enderecoCidade,
        String enderecoDescricao,
        String enderecoComplemento,
        String enderecoCep
) {}
