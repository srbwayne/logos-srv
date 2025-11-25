package com.josecjuniors.logossrv.core.debuff.application.dto;

import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;

import java.util.UUID;

public record DebuffDto(
        UUID id,
        String nome
) {
    public static DebuffDto fromDomain(Debuff domain) {
        return new DebuffDto(domain.getId().getValue(), domain.getNome());
    }
}
