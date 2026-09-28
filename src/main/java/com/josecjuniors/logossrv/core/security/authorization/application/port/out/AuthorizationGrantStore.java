package com.josecjuniors.logossrv.core.security.authorization.application.port.out;

import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationGrant;

import java.util.Optional;

public interface AuthorizationGrantStore {
    enum InsertResult {
        CREATED,
        ALREADY_EXISTS
    }

    InsertResult insertIfAbsent(AuthorizationGrant grant);

    Optional<AuthorizationGrant> findExact(AuthorizationGrant grant);
}
