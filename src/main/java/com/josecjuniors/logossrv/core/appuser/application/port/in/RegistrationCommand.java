package com.josecjuniors.logossrv.core.appuser.application.port.in;

public record RegistrationCommand(
        String email,
        String password,
        String nomeExibicao
) {}
