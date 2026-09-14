package com.josecjuniors.logossrv.adapters.out.atividadeconfig.jdbc;

import com.josecjuniors.logossrv.core.atividadeconfig.application.port.out.LegacyActivityProgressionStateQuery;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLegacyActivityProgressionStateQuery implements LegacyActivityProgressionStateQuery {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLegacyActivityProgressionStateQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsForActivity(AtividadeConfigId id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists (select 1 from regra_distribuicao_atividade where atividade_config_id = ?)",
                Boolean.class,
                id.getValue()));
    }
}
