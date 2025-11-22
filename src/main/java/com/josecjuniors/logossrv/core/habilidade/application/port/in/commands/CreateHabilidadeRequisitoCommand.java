package com.josecjuniors.logossrv.core.habilidade.application.port.in.commands;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;

public record CreateHabilidadeRequisitoCommand(
        Habilidade habilidade,
        Atributo atributoRequisito, // Nulo se não for ATRIBUTO
        Habilidade habilidadeRequisito, // Nulo se não for HABILIDADE
        Integer nivelMinimo
) {}
