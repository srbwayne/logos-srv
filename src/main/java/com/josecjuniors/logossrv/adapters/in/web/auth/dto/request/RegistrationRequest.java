package com.josecjuniors.logossrv.adapters.in.web.auth.dto.request;

public record RegistrationRequest(
        String email,
        String password,
        String nomeExibicao
) {}
