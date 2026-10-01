package com.josecjuniors.logossrv.adapters.out.security.authorization;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.exception.AuthorizationRegistryUnavailableException;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAuthorizationGrantStore implements AuthorizationGrantStore {
    private static final String INSERT = """
            INSERT INTO authorization_grant (principal_type, principal_id, operation, source, namespace)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_authorization_grant_semantic DO NOTHING
            RETURNING id
            """;

    private static final String FIND_EXACT = """
            SELECT principal_type, principal_id, operation, source, namespace
            FROM authorization_grant
            WHERE principal_type = ?
              AND principal_id = ?
              AND operation = ?
              AND source IS NOT DISTINCT FROM ?
              AND namespace IS NOT DISTINCT FROM ?
            """;

    private final JdbcTemplate jdbc;

    public JdbcAuthorizationGrantStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InsertResult insertIfAbsent(AuthorizationGrant grant) {
        List<String> inserted = jdbc.query(INSERT, (rs, row) -> rs.getString("id"),
                grant.principal().principalType().name(), grant.principal().principalId(), grant.operation().name(),
                grant.source().map(AuthorizationSource::value).orElse(null),
                grant.namespace().map(AuthorizationNamespace::value).orElse(null));
        return inserted.isEmpty() ? InsertResult.ALREADY_EXISTS : InsertResult.CREATED;
    }

    @Override
    public Optional<AuthorizationGrant> findExact(AuthorizationGrant grant) {
        try {
            List<AuthorizationGrant> found = jdbc.query(FIND_EXACT, (rs, row) -> {
                String source = rs.getString("source");
                String namespace = rs.getString("namespace");
                return new AuthorizationGrant(
                        new AuthorizationPrincipal(PrincipalType.valueOf(rs.getString("principal_type")),
                                rs.getString("principal_id")),
                        AuthorizationOperation.valueOf(rs.getString("operation")),
                        Optional.ofNullable(source).map(AuthorizationSource::new),
                        Optional.ofNullable(namespace).map(AuthorizationNamespace::new));
            }, grant.principal().principalType().name(), grant.principal().principalId(), grant.operation().name(),
                    grant.source().map(AuthorizationSource::value).orElse(null),
                    grant.namespace().map(AuthorizationNamespace::value).orElse(null));
            return found.stream().findFirst();
        } catch (DataAccessException unavailable) {
            throw new AuthorizationRegistryUnavailableException(unavailable);
        }
    }
}
