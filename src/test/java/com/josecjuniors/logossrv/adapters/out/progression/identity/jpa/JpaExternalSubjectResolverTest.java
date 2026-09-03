package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaExternalSubjectResolverTest {

    @Mock
    private ProgressionSubjectIdentityJpaRepository repository;

    @Test
    void resolveMappingRetornaSubjectIdDoAppUserDoJogador() {
        UUID appUserId = UUID.randomUUID();
        var user = new AppUser(new AppUserId(appUserId), "user@example.com", "password");
        var jogador = new Jogador(JogadorId.generate(), user, "jogador");
        var identity = new ProgressionSubjectIdentity(UUID.randomUUID(), "lifeos", "ABC123", jogador);
        when(repository.findByNamespaceAndExternalId("lifeos", "ABC123")).thenReturn(Optional.of(identity));

        var subjectId = new JpaExternalSubjectResolver(repository)
                .resolve(new ExternalSubjectReference(" LifeOS ", " ABC123 "));

        assertThat(subjectId.value()).isEqualTo(appUserId);
    }

    @Test
    void mappingAusenteRetornaNotFound() {
        when(repository.findByNamespaceAndExternalId("lifeos", "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new JpaExternalSubjectResolver(repository)
                .resolve(new ExternalSubjectReference("lifeos", "missing")))
                .isInstanceOf(ProgressionSubjectNotFoundException.class);
    }

    @Test
    void identidadesDiferentesPodemResolverOMesmoJogador() {
        UUID appUserId = UUID.randomUUID();
        var user = new AppUser(new AppUserId(appUserId), "user@example.com", "password");
        var jogador = new Jogador(JogadorId.generate(), user, "jogador");
        var nativeIdentity = new ProgressionSubjectIdentity(UUID.randomUUID(), "logos-native", "A", jogador);
        var lifeOsIdentity = new ProgressionSubjectIdentity(UUID.randomUUID(), "lifeos", "B", jogador);
        when(repository.findByNamespaceAndExternalId("logos-native", "A"))
                .thenReturn(Optional.of(nativeIdentity));
        when(repository.findByNamespaceAndExternalId("lifeos", "B"))
                .thenReturn(Optional.of(lifeOsIdentity));
        var resolver = new JpaExternalSubjectResolver(repository);

        assertThat(resolver.resolve(new ExternalSubjectReference("logos-native", "A")).value())
                .isEqualTo(appUserId);
        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "B")).value())
                .isEqualTo(appUserId);
    }
}
