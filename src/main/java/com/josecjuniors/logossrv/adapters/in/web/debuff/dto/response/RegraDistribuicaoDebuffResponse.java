package com.josecjuniors.logossrv.adapters.in.web.debuff.dto.response;

import com.josecjuniors.logossrv.core.debuff.application.dto.RegraDistribuicaoDebuffDto;

import java.util.UUID;

public record RegraDistribuicaoDebuffResponse(
        UUID id,
        String debuffNome,
        String atributoNome
) {
    public static RegraDistribuicaoDebuffResponse fromDto(RegraDistribuicaoDebuffDto dto) {
        return new RegraDistribuicaoDebuffResponse(
                dto.id(),
                dto.debuffNome(),
                dto.atributoNome()
        );
    }
}
