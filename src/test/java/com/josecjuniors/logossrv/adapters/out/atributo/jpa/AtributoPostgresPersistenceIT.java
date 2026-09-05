package com.josecjuniors.logossrv.adapters.out.atributo.jpa;

import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AtributoPostgresPersistenceIT {

    @Autowired
    private AtributoJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteCarregaAtualizaERecarregaDescricaoTextual() {
        var id = AtributoId.generate();
        var atributo = new Atributo(id, "LEARNING-IT-" + id.getValue(),
                "Accumulated progression for intentional learning activities.");

        repository.saveAndFlush(atributo);
        entityManager.clear();

        var loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getDescricao()).isEqualTo(atributo.getDescricao());

        loaded.atualizarDescricao("Updated learning progression description.");
        repository.saveAndFlush(loaded);
        entityManager.clear();

        assertThat(repository.findById(id).orElseThrow().getDescricao())
                .isEqualTo("Updated learning progression description.");
        repository.deleteById(id);
        repository.flush();
    }

    @Test
    void preservaDescricaoNula() {
        var id = AtributoId.generate();
        repository.saveAndFlush(new Atributo(id, "LEARNING-NULL-" + id.getValue(), null));
        entityManager.clear();

        assertThat(repository.findById(id).orElseThrow().getDescricao()).isNull();
        repository.deleteById(id);
        repository.flush();
    }
}
