package com.josecjuniors.logossrv.adapters.out.security.workload.replay;

import com.josecjuniors.logossrv.core.security.workload.application.exception.WorkloadReplayStoreUnavailableException;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.ReplayConsumeResult;
import com.josecjuniors.logossrv.core.security.workload.application.port.out.WorkloadAssertionReplayStore;
import com.josecjuniors.logossrv.core.security.workload.domain.WorkloadIssuer;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcWorkloadAssertionReplayStore implements WorkloadAssertionReplayStore {
    private static final String CONSUME_SQL = """
            INSERT INTO workload_assertion_replay (issuer, jti, expires_at)
            VALUES (?, ?, ?)
            ON CONFLICT (issuer, jti) DO NOTHING
            """;
    private static final String DELETE_EXPIRED_SQL = """
            WITH expired AS (
                SELECT issuer, jti
                FROM workload_assertion_replay
                WHERE expires_at < ?
                ORDER BY expires_at
                LIMIT ?
            )
            DELETE FROM workload_assertion_replay replay
            USING expired
            WHERE replay.issuer = expired.issuer
              AND replay.jti = expired.jti
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcWorkloadAssertionReplayStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ReplayConsumeResult consume(WorkloadIssuer issuer, UUID jti, Instant expiresAt) {
        if (issuer == null || jti == null || expiresAt == null) {
            throw new IllegalArgumentException("Replay identity and expiration are required");
        }
        try {
            int inserted = jdbcTemplate.update(CONSUME_SQL, issuer.value(), jti, Timestamp.from(expiresAt));
            return switch (inserted) {
                case 1 -> ReplayConsumeResult.FIRST_USE;
                case 0 -> ReplayConsumeResult.REPLAY;
                default -> throw new WorkloadReplayStoreUnavailableException();
            };
        } catch (DataAccessException exception) {
            throw new WorkloadReplayStoreUnavailableException(exception);
        }
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff, int limit) {
        if (cutoff == null || limit <= 0) {
            throw new IllegalArgumentException("Cutoff and positive batch limit are required");
        }
        try {
            return jdbcTemplate.update(DELETE_EXPIRED_SQL, Timestamp.from(cutoff), limit);
        } catch (DataAccessException exception) {
            throw new WorkloadReplayStoreUnavailableException(exception);
        }
    }
}
