package com.josecjuniors.logossrv.core.progressionconfiguration;

import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.progressionconfiguration.application.service.ProgressionConfigurationAuthoringService;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.model.ProgressionConfigurationDefinition;
import com.josecjuniors.logossrv.core.progressionconfiguration.domain.repository.ProgressionConfigurationAuthoringRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProgressionConfigurationAuthoringServiceTest {
    @Test
    void normalizesLogicalKeyBeforeCreatingDefinition() {
        var repository = mock(ProgressionConfigurationAuthoringRepository.class);
        var service = new ProgressionConfigurationAuthoringService(repository, mock(FatorCalculoRepository.class), mock(AtributoRepository.class));
        var expected = new ProgressionConfigurationDefinition(UUID.randomUUID(), "daily_reading", null, 0L, false);
        when(repository.create("daily_reading")).thenReturn(expected);

        assertThat(service.create("  DAILY_READING ")).isEqualTo(expected);
        verify(repository).create("daily_reading");
    }

}
