package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionResult;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class ProgressionSemanticSnapshotPostgresTest {
    @Autowired
    private AtributoRepository atributos;

    @Autowired
    private ProgressionOutcomeAttributeSemanticSnapshotter snapshotter;

    @Test
    void catalogAssignmentAfterExecutionDoesNotRewriteOldSnapshot() {
        Atributo atributo = atributos.save(new Atributo(new AtributoId(), "Snapshot Catalog Attribute", null));
        String key = atributo.getId().getValue().toString();
        ProgressionOutcome calculated = new ProgressionOutcome(
                new ProgressionResult(3, 0, List.of(new ProgressionResult.AttributeProgression(key, 3))),
                new ProgressionProfile(3, 1, 0, 0,
                        List.of(new ProgressionProfile.ProgressionAttribute(key, 3, 1)), List.of()));

        ProgressionOutcome oldDurableSnapshot = snapshotter.snapshot(calculated);
        atributo.assignSemanticKey("snapshot_knowledge");
        atributos.save(atributo);

        assertThat(oldDurableSnapshot.attributeSemanticKeys()).isEmpty();
        assertThat(snapshotter.snapshot(calculated).attributeSemanticKeys())
                .containsEntry(key, "snapshot_knowledge");
    }
}
