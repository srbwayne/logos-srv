package com.josecjuniors.logossrv.core.habilidade.application.dto;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

public record HabilidadeRequisitoDto(
        String id,
        TipoRequisito tipo,
        String nomeRequisito,
        Integer nivelMinimo
) {
    public static HabilidadeRequisitoDto fromDomain(HabilidadeRequisito requisito) {
        String nome = requisito.getTipoRequisito() == TipoRequisito.ATRIBUTO
                ? requisito.getAtributoRequisito().getNome()
                : requisito.getHabilidadeRequisito().getNome();

        return new HabilidadeRequisitoDto(
                requisito.getId().getValue().toString(),
                requisito.getTipoRequisito(),
                nome,
                requisito.getNivelMinimo()
        );
    }
}
