package com.josecjuniors.logossrv.config.security.authorization;

import com.josecjuniors.logossrv.core.security.authorization.application.AuthorizationEvaluator;
import com.josecjuniors.logossrv.core.security.authorization.application.port.out.AuthorizationGrantStore;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = false)
public class WorkloadAuthorizationMethodSecurityConfig {
    @Bean
    AuthorizationEvaluator authorizationEvaluator(AuthorizationGrantStore grantStore) {
        return new AuthorizationEvaluator(grantStore);
    }

    @Bean
    ProgressionExecuteAuthorizationManager progressionExecuteAuthorizationManager(AuthorizationEvaluator evaluator) {
        return new ProgressionExecuteAuthorizationManager(evaluator);
    }

    @Bean(name = "progressionExecuteAuthorizationAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(name = "logos.security.workload.http.enabled", havingValue = "true", matchIfMissing = false)
    Advisor progressionExecuteAuthorizationAdvisor(ProgressionExecuteAuthorizationManager manager) {
        return new AuthorizationManagerBeforeMethodInterceptor(
                AnnotationMatchingPointcut.forMethodAnnotation(AuthorizeProgressionExecute.class), manager);
    }
}
