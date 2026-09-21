package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoSemanticKeyJaAtribuidaException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssignAtributoSemanticKeyServiceTest {
    @Test
    void assignsAndNormalizesKey() {
        AtributoId id = new AtributoId();
        Atributo atributo = new Atributo(id, "Conhecimento", null);
        AtributoRepository repository = mock(AtributoRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(atributo));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AssignAtributoSemanticKeyService service = new AssignAtributoSemanticKeyService(repository);

        assertThat(service.assign(id, " Knowledge ").semanticKey()).isEqualTo("knowledge");
        verify(repository).save(atributo);
    }

    @Test
    void rejectsChangingAssignedKey() {
        AtributoId id = new AtributoId();
        Atributo atributo = new Atributo(id, "Conhecimento", null, "knowledge");
        AtributoRepository repository = mock(AtributoRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(atributo));
        when(repository.existsBySemanticKeyAndIdNot(any(), eq(id))).thenReturn(false);

        assertThatThrownBy(() -> new AssignAtributoSemanticKeyService(repository).assign(id, "intelligence"))
                .isInstanceOf(AtributoSemanticKeyJaAtribuidaException.class);
        verify(repository, never()).save(any());
    }
}
