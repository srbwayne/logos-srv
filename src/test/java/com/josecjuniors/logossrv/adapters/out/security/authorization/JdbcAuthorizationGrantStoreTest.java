package com.josecjuniors.logossrv.adapters.out.security.authorization;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.exception.AuthorizationRegistryUnavailableException;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationPrincipal;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcAuthorizationGrantStoreTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void translatesDataAccessFailureToBoundedRegistryUnavailableException() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DataAccessResourceFailureException databaseFailure = new DataAccessResourceFailureException("private database detail");
        doThrow(databaseFailure).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        JdbcAuthorizationGrantStore store = new JdbcAuthorizationGrantStore(jdbc);

        assertThatThrownBy(() -> store.findExact(grant()))
                .isInstanceOf(AuthorizationRegistryUnavailableException.class)
                .hasMessage("Authorization registry is unavailable.")
                .hasCause(databaseFailure);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void missingRowRemainsAnOrdinaryEmptyResult() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        JdbcAuthorizationGrantStore store = new JdbcAuthorizationGrantStore(jdbc);

        assertThat(store.findExact(grant())).isEqualTo(Optional.empty());
    }

    private static AuthorizationGrant grant() {
        return new AuthorizationGrant(new AuthorizationPrincipal(PrincipalType.WORKLOAD, "lifeos"),
                AuthorizationOperation.PROGRESSION_EXECUTE, Optional.of(new AuthorizationSource("lifeos")),
                Optional.of(new AuthorizationNamespace("lifeos")));
    }
}
