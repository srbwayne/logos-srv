package com.josecjuniors.logossrv.core.subjectownership.application.port.out;

import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;

import java.util.Objects;

/** Immutable server-derived authority and audit identity for an ownership operator. */
public record AuthorizedSubjectOwnershipOperator(
        PrincipalType principalType,
        String principalId,
        String namespace) {

    public static final String AUDIT_ACTOR_TYPE = "WORKLOAD_OPERATOR";

    public AuthorizedSubjectOwnershipOperator {
        Objects.requireNonNull(principalType, "principalType");
        if (principalType != PrincipalType.WORKLOAD) {
            throw new IllegalArgumentException("Only WORKLOAD principals may manage subject ownership");
        }
        if (principalId == null || principalId.isBlank() || !principalId.equals(principalId.trim())
                || principalId.length() > 128) {
            throw new IllegalArgumentException("principalId must be trimmed, nonblank, and at most 128 characters");
        }
        Objects.requireNonNull(namespace, "namespace");
        String normalizedNamespace = new AuthorizationNamespace(namespace).value();
        if (!namespace.equals(normalizedNamespace)) {
            throw new IllegalArgumentException("namespace must already be canonical");
        }
    }

    public String auditActorType() {
        return AUDIT_ACTOR_TYPE;
    }
}
