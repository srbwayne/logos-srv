package com.josecjuniors.logossrv.core.debuff.application.dto;

import com.josecjuniors.logossrv.core.debuff.domain.exception.RegraDistribuicaoDebuffNaoPertenceADebuffException;
import com.josecjuniors.logossrv.core.debuff.domain.model.RegraDistribuicaoDebuff;

import java.util.UUID;

public record RegraDistribuicaoDebuffDto(
        UUID id,
        UUID debuffId,
        String debuffNome,
        UUID atributoId,
        String atributoNome
) {
    public static RegraDistribuicaoDebuffDto fromDomain(RegraDistribuicaoDebuff domain) {

        return new RegraDistribuicaoDebuffDto(
                domain.getId().getValue(),
                domain.getDebuff().getId().getValue(),
                domain.getDebuff().getNome(),
                domain.getAtributo().getId().getValue(),
                domain.getAtributo().getNome()
        );
    }
}
