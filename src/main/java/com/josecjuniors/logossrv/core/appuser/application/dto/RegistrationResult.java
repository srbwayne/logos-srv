package com.josecjuniors.logossrv.core.appuser.application.dto;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;

public record RegistrationResult(AppUser user, Jogador jogador) {}
