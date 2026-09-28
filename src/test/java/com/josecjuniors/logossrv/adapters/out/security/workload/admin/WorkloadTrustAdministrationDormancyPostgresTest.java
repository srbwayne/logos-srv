package com.josecjuniors.logossrv.adapters.out.security.workload.admin;

import com.josecjuniors.logossrv.core.security.workload.admin.application.WorkloadTrustAdministrationService;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class WorkloadTrustAdministrationDormancyPostgresTest {
    @Autowired ApplicationContext applicationContext;
    @Autowired JdbcTemplate jdbc;

    @Test
    void administrationCapabilityHasNoInboundHttpOrStartupBootstrap() {
        assertThat(applicationContext.getBeansOfType(WorkloadTrustAdministrationService.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
        assertThat(applicationContext.getBeansWithAnnotation(RestController.class).keySet())
                .noneMatch(beanName -> beanName.toLowerCase().contains("trustadministration"));
        assertThat(applicationContext.getBeanDefinitionNames())
                .noneMatch(beanName -> beanName.toLowerCase().contains("trustbootstrap"));

        assertThat(count("workload_principal")).isZero();
        assertThat(count("workload_signing_key")).isZero();
        assertThat(count("workload_trust_audit_event")).isZero();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
