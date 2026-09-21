package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProgressionOutcomeAttributeSemanticSnapshotterTest {
    @Test
    void snapshotsResultAndProfileKeysOnceAndIgnoresNullKeys() {
        AtributoId id = new AtributoId();
        Atributo known = new Atributo(id, "Conhecimento", null, "knowledge");
        AtributoRepository repository = mock(AtributoRepository.class);
        when(repository.findAll()).thenReturn(List.of(known));
        var outcome = new ProgressionOutcome(
                new ProgressionResult(30, 0, List.of(new ProgressionResult.AttributeProgression(id.getValue().toString(), 30))),
                new ProgressionProfile(30, 1, 0, 0,
                        List.of(new ProgressionProfile.ProgressionAttribute(id.getValue().toString(), 30, 1)), List.of()));

        var snapshot = new ProgressionOutcomeAttributeSemanticSnapshotter(repository).snapshot(outcome);

        assertThat(snapshot.attributeSemanticKeys()).containsEntry(id.getValue().toString(), "knowledge");
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void snapshotRemainsStableAfterCatalogAssignment() {
        AtributoId id = new AtributoId();
        Atributo legacy = new Atributo(id, "Conhecimento", null);
        AtributoRepository repository = mock(AtributoRepository.class);
        when(repository.findAll()).thenReturn(List.of(legacy));
        var outcome = new ProgressionOutcome(
                new ProgressionResult(1, 0, List.of(new ProgressionResult.AttributeProgression(id.getValue().toString(), 1))),
                new ProgressionProfile(1, 1, 0, 0, List.of(), List.of()));

        var durable = new ProgressionOutcomeAttributeSemanticSnapshotter(repository).snapshot(outcome);
        legacy.assignSemanticKey("knowledge");

        assertThat(durable.attributeSemanticKeys()).isEmpty();
    }
}
