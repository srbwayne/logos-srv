package com.josecjuniors.logossrv.adapters.in.web.regradistribuicaohabilidade.dto.request;

import java.util.UUID;

public record CreateRegraDistribuicaoHabilidadeRequest(
        UUID atributoId,
        Double pesoDistribuicao
) {}
