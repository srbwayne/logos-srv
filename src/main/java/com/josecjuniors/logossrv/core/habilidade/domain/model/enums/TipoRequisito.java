package com.josecjuniors.logossrv.core.habilidade.domain.model.enums;

import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.CreateHabilidadeRequisitoCommand;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;

import java.util.function.Function;

@FunctionalInterface
interface RequisitoFactory extends Function<CreateHabilidadeRequisitoCommand, HabilidadeRequisito> {}

public enum TipoRequisito {
    ATRIBUTO(command -> HabilidadeRequisito.paraAtributo(command.habilidade(), command.atributoRequisito(), command.nivelMinimo())),
    HABILIDADE(command -> HabilidadeRequisito.paraHabilidade(command.habilidade(), command.habilidadeRequisito(), command.nivelMinimo())),
    JOGADOR(command -> HabilidadeRequisito.paraJogador(command.habilidade(), command.nivelMinimo()));

    private final RequisitoFactory factory;

    TipoRequisito(RequisitoFactory factory) {
        this.factory = factory;
    }

    public HabilidadeRequisito criar(CreateHabilidadeRequisitoCommand command) {
        return factory.apply(command);
    }
}
