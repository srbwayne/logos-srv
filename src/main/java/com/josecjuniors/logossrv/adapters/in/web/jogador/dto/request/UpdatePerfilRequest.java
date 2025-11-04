package com.josecjuniors.logossrv.adapters.in.web.jogador.dto.request;

import com.josecjuniors.logossrv.core.jogador.domain.model.enums.EnderecoEstado;
import com.josecjuniors.logossrv.core.jogador.domain.model.enums.Sexo;

import java.time.LocalDate;

public record UpdatePerfilRequest(
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
