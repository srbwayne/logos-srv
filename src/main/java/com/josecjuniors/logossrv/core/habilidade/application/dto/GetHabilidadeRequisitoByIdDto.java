package com.josecjuniors.logossrv.core.habilidade.application.dto;

import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;

import java.util.UUID;

public record GetHabilidadeRequisitoByIdDto(
        String id,
        TipoRequisito tipo,
        UUID requisitoId, // ID do Atributo ou Habilidade requisito
        String requisitoNome,
        Integer nivelMinimo
) {
    public static GetHabilidadeRequisitoByIdDto fromDomain(HabilidadeRequisito requisito) {
        UUID reqId;
        String reqNome;

        if (requisito.getTipoRequisito() == TipoRequisito.ATRIBUTO) {
            reqId = requisito.getAtributoRequisito().getId().getValue();
            reqNome = requisito.getAtributoRequisito().getNome();
        } else {
            reqId = requisito.getHabilidadeRequisito().getId().getValue();
            reqNome = requisito.getHabilidadeRequisito().getNome();
        }

        return new GetHabilidadeRequisitoByIdDto(
                requisito.getId().getValue().toString(),
                requisito.getTipoRequisito(),
                reqId,
                reqNome,
                requisito.getNivelMinimo()
        );
    }
}
