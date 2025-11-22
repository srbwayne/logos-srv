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
        String nome = "";
        if (requisito.getTipoRequisito() == TipoRequisito.ATRIBUTO) {
            nome = requisito.getAtributoRequisito().getNome();
        } else if (requisito.getTipoRequisito() == TipoRequisito.HABILIDADE) {
            nome = requisito.getHabilidadeRequisito().getNome();
        } else if (requisito.getTipoRequisito() == TipoRequisito.JOGADOR) {
            nome = "Nível do Jogador";
        }
        return new HabilidadeRequisitoDto(requisito.getId().getValue().toString(), requisito.getTipoRequisito(), nome, requisito.getNivelMinimo());
    }
}
