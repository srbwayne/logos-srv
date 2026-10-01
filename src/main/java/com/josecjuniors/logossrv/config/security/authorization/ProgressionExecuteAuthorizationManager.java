package com.josecjuniors.logossrv.config.security.authorization;

import com.josecjuniors.logossrv.adapters.in.web.progression.dto.request.ProgressionExecutionRequest;
import com.josecjuniors.logossrv.config.security.workload.WorkloadPrincipalAuthenticationToken;
import com.josecjuniors.logossrv.core.security.authentication.domain.AuthenticatedPrincipal;
import com.josecjuniors.logossrv.core.security.authentication.domain.PrincipalType;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationVerdict;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationNamespace;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationOperation;
import com.josecjuniors.logossrv.core.security.authorization.domain.AuthorizationSource;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public final class ProgressionExecuteAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    private final AuthorizationEvaluator evaluator;

    public ProgressionExecuteAuthorizationManager(AuthorizationEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, MethodInvocation invocation) {
        Authentication authentication = authenticationSupplier == null ? null : authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication instanceof WorkloadPrincipalAuthenticationToken)) {
            return deny();
        }
        Object principalValue = authentication.getPrincipal();
        if (!(principalValue instanceof AuthenticatedPrincipal principal)
                || principal.principalType() != PrincipalType.WORKLOAD) {
            return deny();
        }

        ProgressionExecutionRequest request = Arrays.stream(invocation.getArguments())
                .filter(ProgressionExecutionRequest.class::isInstance)
                .map(ProgressionExecutionRequest.class::cast)
                .findFirst()
                .orElse(null);
        if (request == null || request.execution() == null || request.subject() == null
                || request.execution().source() == null || request.subject().namespace() == null) {
            return deny();
        }

        final AuthorizationSource source;
        final AuthorizationNamespace namespace;
        try {
            source = new AuthorizationSource(request.execution().source());
            namespace = new AuthorizationNamespace(request.subject().namespace());
        } catch (IllegalArgumentException invalidAuthorizationDimension) {
            return deny();
        }

        AuthorizationVerdict verdict = evaluator.evaluate(principal, AuthorizationOperation.PROGRESSION_EXECUTE,
                Optional.of(source), Optional.of(namespace));
        return new AuthorizationDecision(verdict == AuthorizationVerdict.ALLOW);
    }

    private static AuthorizationDecision deny() {
        return new AuthorizationDecision(false);
    }
}
