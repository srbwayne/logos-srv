package com.josecjuniors.logossrv.adapters.out.progression.identity.jpa;

import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
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
        when(repository.findCurrentAppUserId("lifeos", "ABC123"))
                .thenReturn(Optional.of(appUserId));

        var subjectId = new JpaExternalSubjectResolver(repository)
                .resolve(new ExternalSubjectReference(" LifeOS ", " ABC123 "));

        assertThat(subjectId.value()).isEqualTo(appUserId);
    }

    @Test
    void mappingAusenteRetornaNotFound() {
        when(repository.findCurrentAppUserId("lifeos", "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new JpaExternalSubjectResolver(repository)
                .resolve(new ExternalSubjectReference("lifeos", "missing")))
                .isInstanceOf(ProgressionSubjectNotFoundException.class);
    }

    @Test
    void identityHistoryWithoutCurrentPointerFailsAsIntegrityError() {
        when(repository.findCurrentAppUserId("lifeos", "orphan"))
                .thenReturn(Optional.empty());
        when(repository.existsByNamespaceAndExternalId("lifeos", "orphan"))
                .thenReturn(true);

        assertThatThrownBy(() -> new JpaExternalSubjectResolver(repository)
                .resolve(new ExternalSubjectReference("lifeos", "orphan")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("without a current binding pointer");
    }

    @Test
    void identidadesDiferentesPodemResolverOMesmoJogador() {
        UUID appUserId = UUID.randomUUID();
        when(repository.findCurrentAppUserId("logos-native", "A"))
                .thenReturn(Optional.of(appUserId));
        when(repository.findCurrentAppUserId("lifeos", "B"))
                .thenReturn(Optional.of(appUserId));
        var resolver = new JpaExternalSubjectResolver(repository);

        assertThat(resolver.resolve(new ExternalSubjectReference("logos-native", "A")).value())
                .isEqualTo(appUserId);
        assertThat(resolver.resolve(new ExternalSubjectReference("lifeos", "B")).value())
                .isEqualTo(appUserId);
    }
}
