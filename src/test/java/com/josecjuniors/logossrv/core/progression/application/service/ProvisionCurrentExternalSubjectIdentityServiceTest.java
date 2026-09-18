package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.exception.JogadorNaoEncontradoException;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionSubjectIdentityProvisioningPort;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProvisionCurrentExternalSubjectIdentityServiceTest {

    @Test
    void provisionsNormalizedReferenceForCurrentPlayer() {
        var appUserId = UUID.randomUUID();
        var user = new AppUser(new AppUserId(appUserId), "user@example.com", "password");
        var jogador = new Jogador(JogadorId.generate(), user, "player");
        var jogadores = mock(JogadorRepository.class);
        var provisioning = mock(ProgressionSubjectIdentityProvisioningPort.class);
        when(jogadores.findByUserEmail("user@example.com")).thenReturn(java.util.Optional.of(jogador));

        var reference = new ProvisionCurrentExternalSubjectIdentityService(jogadores, provisioning)
                .provision("user@example.com", new ExternalSubjectReference(" LIFEOS ", " user-123 "));

        assertThat(reference.namespace()).isEqualTo("lifeos");
        assertThat(reference.externalId()).isEqualTo("user-123");
        verify(provisioning).provision(eq(reference), eq(new SubjectId(appUserId)));
    }

    @Test
    void rejectsLogosNativeNamespace() {
        var provisioning = mock(ProgressionSubjectIdentityProvisioningPort.class);
        var service = new ProvisionCurrentExternalSubjectIdentityService(mock(JogadorRepository.class), provisioning);

        assertThatThrownBy(() -> service.provision("user@example.com",
                new ExternalSubjectReference("logos-native", "anything")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingCurrentPlayerFailsWithoutProvisioning() {
        var jogadores = mock(JogadorRepository.class);
        var provisioning = mock(ProgressionSubjectIdentityProvisioningPort.class);
        when(jogadores.findByUserEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> new ProvisionCurrentExternalSubjectIdentityService(jogadores, provisioning)
                .provision("missing@example.com", new ExternalSubjectReference("lifeos", "user-123")))
                .isInstanceOf(JogadorNaoEncontradoException.class);
    }
}
