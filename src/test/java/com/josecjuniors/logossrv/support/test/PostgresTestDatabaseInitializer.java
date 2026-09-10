package com.josecjuniors.logossrv.support.test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.MapPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates one disposable PostgreSQL schema for the Spring test context.
 *
 * <p>This is test infrastructure only. Flyway still owns schema creation and
 * migration; this initializer only creates the isolated namespace and points
 * the test datasource at it.</p>
 */
public final class PostgresTestDatabaseInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Object LIFECYCLE_LOCK = new Object();
    private static SchemaLease activeLease;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var environment = context.getEnvironment();
        var baseUrl = withoutCurrentSchema(runtimeValue(
                "LOGOS_TEST_DB_URL", environment.getProperty("spring.datasource.url")));
        var username = runtimeValue(
                "LOGOS_TEST_DB_USERNAME", environment.getProperty("spring.datasource.username"));
        var password = runtimeValue(
                "LOGOS_TEST_DB_PASSWORD", environment.getProperty("spring.datasource.password"));

        validateDatabaseUrl(baseUrl);

        SchemaLease lease = acquire(baseUrl, username, password);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "isolated-postgres-test-schema",
                Map.of(
                        "spring.datasource.url", withCurrentSchema(baseUrl, lease.schemaName),
                        "spring.datasource.username", username,
                        "spring.datasource.password", password,
                        "spring.flyway.schemas", lease.schemaName,
                        "spring.flyway.default-schema", lease.schemaName,
                        "spring.flyway.create-schemas", "true"
                )));

        var cleanup = new CleanupHandle(lease);
        context.addApplicationListener(event -> {
            if (event instanceof ContextRefreshedEvent refreshed
                    && refreshed.getApplicationContext() == context) {
                lease.bootstrapRequiredFixtures();
            }
            if (event instanceof ContextClosedEvent) {
                cleanup.destroy();
            }
        });
    }

    private static SchemaLease acquire(String baseUrl, String username, String password) {
        synchronized (LIFECYCLE_LOCK) {
            if (activeLease == null) {
                activeLease = new SchemaLease(baseUrl, username, password);
                activeLease.create();
            }
            activeLease.references.incrementAndGet();
            return activeLease;
        }
    }

    private static void release(SchemaLease lease) {
        synchronized (LIFECYCLE_LOCK) {
            if (lease.references.decrementAndGet() == 0) {
                lease.drop();
                activeLease = null;
            }
        }
    }

    private static void validateDatabaseUrl(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "PostgreSQL test isolation requires a jdbc:postgresql URL");
        }
        if (url.contains("logos_task010r_validation")) {
            throw new IllegalStateException(
                    "The controlled E2E database cannot be used by the Maven test lifecycle");
        }
    }

    private static String runtimeValue(String name, String fallback) {
        var value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String withoutCurrentSchema(String url) {
        var separator = url.indexOf('?');
        if (separator < 0) {
            return url;
        }
        var base = url.substring(0, separator);
        var parameters = url.substring(separator + 1).lines().findFirst().orElse("")
                .replaceAll("(^|&)currentSchema=[^&]*&?", "")
                .replaceAll("&&", "&")
                .replaceAll("(^&|&$)", "");
        return parameters.isBlank() ? base : base + "?" + parameters;
    }

    private static String withCurrentSchema(String url, String schema) {
        return url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static final class SchemaLease {
        private final String baseUrl;
        private final String username;
        private final String password;
        private final String schemaName = "logos_test_" + UUID.randomUUID().toString().replace("-", "");
        private final AtomicInteger references = new AtomicInteger();

        private SchemaLease(String baseUrl, String username, String password) {
            this.baseUrl = baseUrl;
            this.username = username;
            this.password = password;
        }

        private void create() {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + quoteIdentifier(schemaName));
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not create isolated PostgreSQL test schema", exception);
            }
        }

        private void bootstrapRequiredFixtures() {
            try (Connection connection = DriverManager.getConnection(
                    withCurrentSchema(baseUrl, schemaName), username, password);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO atributo (id, nome)
                        VALUES ('11111111-1111-4111-8111-111111111111', 'LEARNING')
                        ON CONFLICT (nome) DO NOTHING
                        """);
                statement.executeUpdate("""
                        INSERT INTO progression_configuration_definition
                            (id, logical_key, legacy_atividade_config_id, current_version_id)
                        VALUES ('33333333-3333-4333-8333-333333333333', 'reading', NULL, NULL)
                        ON CONFLICT (logical_key) DO NOTHING
                        """);
                statement.executeUpdate("""
                        INSERT INTO progression_configuration_version
                            (id, definition_id, revision, base_xp, base_stress, fact_key_generation)
                        VALUES
                            ('44444444-4444-4444-8444-444444444444',
                             '33333333-3333-4333-8333-333333333333', 1, 1, 0, 'SEMANTIC'),
                            ('55555555-5555-4555-8555-555555555555',
                             '33333333-3333-4333-8333-333333333333', 2, 1, 0, 'SEMANTIC')
                        ON CONFLICT (definition_id, revision) DO NOTHING
                        """);
                statement.executeUpdate("""
                        UPDATE progression_configuration_definition
                        SET current_version_id = '55555555-5555-4555-8555-555555555555'
                        WHERE logical_key = 'reading'
                        """);
                statement.executeUpdate("""
                        INSERT INTO progression_configuration_version_distribution
                            (id, configuration_version_id, attribute_key, weight)
                        VALUES
                            ('66666666-6666-4666-8666-666666666666',
                             '44444444-4444-4444-8444-444444444444',
                             '11111111-1111-4111-8111-111111111111', 1.0),
                            ('77777777-7777-4777-8777-777777777777',
                             '55555555-5555-4555-8555-555555555555',
                             '11111111-1111-4111-8111-111111111111', 1.0)
                        ON CONFLICT (id) DO NOTHING
                        """);
                statement.executeUpdate("""
                        INSERT INTO progression_configuration_version_xp_rule
                            (id, distribution_id, factor_key, multiplier, min_cutoff,
                             max_cutoff, calculation_mode)
                        VALUES
                            ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
                             '66666666-6666-4666-8666-666666666666', 'pages_read', 1.0, NULL, NULL, 'FIXED'),
                            ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
                             '77777777-7777-4777-8777-777777777777', 'pages_read', 1.0, NULL, NULL, 'FACT_VALUE')
                        ON CONFLICT (id) DO NOTHING
                        """);
                statement.executeUpdate("""
                        INSERT INTO progression_configuration_version_factor
                            (id, configuration_version_id, factor_key, tipo_input)
                        VALUES
                            ('cccccccc-cccc-4ccc-8ccc-cccccccccccc',
                             '44444444-4444-4444-8444-444444444444', 'pages_read', 'NUMERICO'),
                            ('dddddddd-dddd-4ddd-8ddd-dddddddddddd',
                             '55555555-5555-4555-8555-555555555555', 'pages_read', 'NUMERICO')
                        ON CONFLICT (configuration_version_id, factor_key) DO NOTHING
                        """);
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Could not bootstrap required PostgreSQL test fixtures", exception);
            }
        }

        private void drop() {
            if (!schemaName.matches("logos_test_[0-9a-f]{32}")) {
                throw new IllegalStateException("Refusing to drop an unrecognized test schema");
            }
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + quoteIdentifier(schemaName) + " CASCADE");
            } catch (SQLException exception) {
                System.err.println("Could not drop isolated PostgreSQL test schema " + schemaName);
            }
        }
    }

    private static final class CleanupHandle {
        private final SchemaLease lease;
        private boolean closed;

        private CleanupHandle(SchemaLease lease) {
            this.lease = lease;
        }

        public synchronized void destroy() {
            if (!closed) {
                closed = true;
                release(lease);
            }
        }
    }
}
