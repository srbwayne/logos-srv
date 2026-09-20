package com.josecjuniors.logossrv.adapters.out.atributo;

import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FreshPostgresIntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AtributoSemanticKeyPostgresTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allowsMultipleNullSemanticKeys() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertThatCode(() -> {
            jdbc.update("INSERT INTO atributo (id, nome, descricao, semantic_key) VALUES (?, ?, ?, NULL)", first, "Legacy " + first, null);
            jdbc.update("INSERT INTO atributo (id, nome, descricao, semantic_key) VALUES (?, ?, ?, NULL)", second, "Legacy " + second, null);
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateNonNullSemanticKey() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String key = "knowledge_" + first.toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO atributo (id, nome, descricao, semantic_key) VALUES (?, ?, ?, ?)", first, "Knowledge " + first, null, key);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO atributo (id, nome, descricao, semantic_key) VALUES (?, ?, ?, ?)",
                second, "Knowledge " + second, null, key))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
