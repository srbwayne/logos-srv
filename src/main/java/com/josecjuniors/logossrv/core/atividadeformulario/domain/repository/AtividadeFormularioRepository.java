package com.josecjuniors.logossrv.core.atividadeformulario.domain.repository;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.AtividadeFormulario;

import java.util.Optional;

public interface AtividadeFormularioRepository {
    AtividadeFormulario save(AtividadeFormulario formulario);
    Optional<AtividadeFormulario> findByAtividadeConfigId(AtividadeConfigId atividadeConfigId);
    void deleteAll(); // Adicionado
}
