package com.josecjuniors.logossrv.support.test;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class ProgressionSubjectIdentityVerificationMigrationPostgresTest {
    @Autowired
    DataSource dataSource;

    @Test
    void v45MarksNativeAndLegacyExternalMappingsWithoutLosingIdentityUniqueness() throws Exception {
        String schema = "subject_link_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (HikariDataSource isolatedDataSource = isolatedDataSource()) {
            Flyway beforeV45 = Flyway.configure().dataSource(isolatedDataSource).schemas(schema)
                    .defaultSchema(schema).target(MigrationVersion.fromVersion("44")).load();
            Flyway throughV45 = Flyway.configure().dataSource(isolatedDataSource).schemas(schema)
                    .defaultSchema(schema).load();
            beforeV45.migrate();
            UUID userId = UUID.randomUUID();
            UUID jogadorId = UUID.randomUUID();
            try (Connection connection = isolatedDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schema + "\"");
                statement.executeUpdate("INSERT INTO app_user(id,email,password) VALUES ('" + userId + "','migration@example.test','test')");
                statement.executeUpdate("INSERT INTO jogador(id,user_id,apelido) VALUES ('" + jogadorId + "','" + userId + "','Migration')");
                statement.executeUpdate("INSERT INTO progression_subject_identity(id,namespace,external_id,jogador_id) VALUES "
                        + "('" + UUID.randomUUID() + "','logos-native','" + userId + "','" + jogadorId + "'),"
                        + "('" + UUID.randomUUID() + "','lifeos','legacy-user','" + jogadorId + "')");
            }
            throughV45.migrate();

            try (Connection connection = isolatedDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schema + "\"");
                try (var rows = statement.executeQuery("SELECT namespace, verification_status FROM progression_subject_identity ORDER BY namespace")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("namespace")).isEqualTo("lifeos");
                    assertThat(rows.getString("verification_status")).isEqualTo("UNVERIFIED");
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("namespace")).isEqualTo("logos-native");
                    assertThat(rows.getString("verification_status")).isEqualTo("LOGOS_NATIVE");
                    assertThat(rows.next()).isFalse();
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_schema='" + schema + "' AND constraint_name='uk_progression_subject_identity_namespace_external_id'")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
                try (var rows = statement.executeQuery("SELECT version FROM flyway_schema_history WHERE success=true ORDER BY installed_rank DESC LIMIT 1")) {
                    rows.next();
                    assertThat(rows.getString(1)).isEqualTo("45");
                }
            }
        } finally {
            try (HikariDataSource isolatedDataSource = isolatedDataSource();
                 Connection connection = isolatedDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
            }
        }
    }

    private HikariDataSource isolatedDataSource() {
        HikariDataSource shared = (HikariDataSource) dataSource;
        String jdbcUrl = shared.getJdbcUrl().replaceAll("([?&])currentSchema=[^&]*", "$1")
                .replace("?&", "?").replaceAll("[?&]$", "");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(shared.getUsername());
        config.setPassword(shared.getPassword());
        config.setMaximumPoolSize(2);
        return new HikariDataSource(config);
    }
}
