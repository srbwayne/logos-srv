package com.josecjuniors.logossrv.config.security.authorization;

import com.josecjuniors.logossrv.config.security.workload.WorkloadPrincipalAuthenticationToken;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticationStatus;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationVerdict;
import com.josecjuniors.logossrv.core.security.authorization.application.exception.AuthorizationRegistryUnavailableException;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.AuthorizedSubjectOwnershipOperator;
import com.josecjuniors.logossrv.core.subjectownership.application.port.out.SubjectOwnershipOperatorContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public final class SecurityContextSubjectOwnershipOperatorContext implements SubjectOwnershipOperatorContext {
    private final AuthorizationEvaluator evaluator;

    public SecurityContextSubjectOwnershipOperatorContext(AuthorizationEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public AuthorizedSubjectOwnershipOperator authorizeForNamespace(String namespace) {
        AuthorizationNamespace authorizedNamespace = canonicalNamespace(namespace);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication instanceof WorkloadPrincipalAuthenticationToken)
                || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)
                || principal.principalType() != PrincipalType.WORKLOAD
                || principal.authenticationStatus() != AuthenticationStatus.VERIFIED
                || !validPrincipalId(principal.principalId())) {
            throw denied();
        }

        AuthorizationVerdict verdict;
        try {
            verdict = evaluator.evaluate(principal, AuthorizationOperation.SUBJECT_OWNERSHIP_MANAGE,
                    Optional.empty(), Optional.of(authorizedNamespace));
        } catch (AuthorizationRegistryUnavailableException | IllegalArgumentException failure) {
            throw denied();
        } catch (RuntimeException failure) {
            throw denied();
        }
        if (verdict != AuthorizationVerdict.ALLOW) {
            throw denied();
        }
        return new AuthorizedSubjectOwnershipOperator(principal.principalType(), principal.principalId(),
                authorizedNamespace.value());
    }

    private static AuthorizationNamespace canonicalNamespace(String namespace) {
        try {
            AuthorizationNamespace parsed = new AuthorizationNamespace(namespace);
            if (!parsed.value().equals(namespace)) {
                throw denied();
            }
            return parsed;
        } catch (RuntimeException invalidNamespace) {
            throw denied();
        }
    }

    private static boolean validPrincipalId(String principalId) {
        return principalId != null && !principalId.isBlank()
                && principalId.equals(principalId.trim()) && principalId.length() <= 128;
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Subject ownership administration is not authorized");
    }
}
