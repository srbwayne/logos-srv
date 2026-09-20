package com.josecjuniors.logossrv.support.test;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring integration test backed by Flyway-only disposable PostgreSQL state.
 * Historical shared fixtures are intentionally not installed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "logos.test.bootstrap-required-fixtures=false")
@ContextConfiguration(initializers = PostgresTestDatabaseInitializer.class)
public @interface FreshPostgresIntegrationTest {
}
