package com.josecjuniors.logossrv.adapters.in.web.auth.dto.response;

public record RegistrationResponse(
        String userId,
        String jogadorId,
        String email,
        String nomeExibicao
) {}
