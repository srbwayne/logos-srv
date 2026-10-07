package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.adapters.out.subjectownership.jpa.ProgressionSubjectOwnershipHistoryJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProgressionSubjectIdentityProvisioningAdapterTest {

    private static final ExternalSubjectReference REFERENCE =
            new ExternalSubjectReference("lifeos", "external-123");

    @Mock
    private ProgressionSubjectIdentityJpaRepository repository;
    @Mock
    private ProgressionSubjectOwnershipHistoryJpaRepository historyRepository;
    @Mock
    private JogadorRepository jogadorRepository;

    @Test
    void rechecksAndLocksPointerWhenConcurrentProvisioningCreatedHistory() {
        UUID targetId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        Jogador target = jogador(targetId);
        ProgressionSubjectIdentity currentIdentity =
                new ProgressionSubjectIdentity(identityId, REFERENCE.namespace(), REFERENCE.externalId(), target);
        when(jogadorRepository.findByAppUserId(any(AppUserId.class)))
                .thenReturn(Optional.of(target));
        when(repository.lockCurrentIdentityId(REFERENCE.namespace(), REFERENCE.externalId()))
                .thenReturn(Optional.empty(), Optional.of(identityId));
        when(repository.existsByNamespaceAndExternalId(REFERENCE.namespace(), REFERENCE.externalId()))
                .thenReturn(true);
        when(repository.findCurrentIdentityByIdForUpdate(identityId))
                .thenReturn(Optional.of(currentIdentity));

        assertThatCode(() -> adapter().provision(REFERENCE, new SubjectId(targetId)))
                .doesNotThrowAnyException();

        InOrder order = inOrder(repository);
        order.verify(repository).lockCurrentIdentityId(REFERENCE.namespace(), REFERENCE.externalId());
        order.verify(repository).existsByNamespaceAndExternalId(REFERENCE.namespace(), REFERENCE.externalId());
        order.verify(repository).lockCurrentIdentityId(REFERENCE.namespace(), REFERENCE.externalId());
        order.verify(repository).findCurrentIdentityByIdForUpdate(identityId);
        verify(repository, never()).insertCurrentBindingIfAbsent(
                eq(REFERENCE.namespace()), eq(REFERENCE.externalId()), any(UUID.class));
    }

    @Test
    void historyWithPointerStillAbsentFailsAsIntegrityError() {
        UUID targetId = UUID.randomUUID();
        when(jogadorRepository.findByAppUserId(any(AppUserId.class)))
                .thenReturn(Optional.of(jogador(targetId)));
        when(repository.lockCurrentIdentityId(REFERENCE.namespace(), REFERENCE.externalId()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(repository.existsByNamespaceAndExternalId(REFERENCE.namespace(), REFERENCE.externalId()))
                .thenReturn(true);

        assertThatThrownBy(() -> adapter().provision(REFERENCE, new SubjectId(targetId)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Subject identity history exists without a current-binding pointer");

        verify(repository, times(2)).lockCurrentIdentityId(REFERENCE.namespace(), REFERENCE.externalId());
        verify(repository, never()).findCurrentIdentityByIdForUpdate(any(UUID.class));
        verify(repository, never()).insertCurrentBindingIfAbsent(
                eq(REFERENCE.namespace()), eq(REFERENCE.externalId()), any(UUID.class));
    }

    private JpaProgressionSubjectIdentityProvisioningAdapter adapter() {
        return new JpaProgressionSubjectIdentityProvisioningAdapter(
                repository, historyRepository, jogadorRepository);
    }

    private Jogador jogador(UUID userId) {
        var user = new AppUser(new AppUserId(userId), "user-" + userId + "@example.com", "encoded");
        return new Jogador(new JogadorId(userId), user, "player-" + userId);
    }
}
