package com.josecjuniors.logossrv.adapters.out.atividadeconfig.jdbc;

import com.josecjuniors.logossrv.core.atividadeconfig.application.port.out.ActivityDeletionDependencyQuery;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcActivityDeletionDependencyQuery implements ActivityDeletionDependencyQuery {
    private final JdbcTemplate jdbcTemplate;

    public JdbcActivityDeletionDependencyQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean hasProgressionDefinition(AtividadeConfigId id) {
        return exists("select exists (select 1 from progression_configuration_definition where legacy_atividade_config_id = ?)", id);
    }

    @Override
    public boolean hasRegistroAtividade(AtividadeConfigId id) {
        return exists("select exists (select 1 from registro_atividade where atividade_config_id = ?)", id);
    }

    @Override
    public boolean hasAtividadeAgendada(AtividadeConfigId id) {
        return exists("select exists (select 1 from atividade_agendada where atividade_config_id = ?)", id);
    }

    private boolean exists(String sql, AtividadeConfigId id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, id.getValue()));
    }
}
