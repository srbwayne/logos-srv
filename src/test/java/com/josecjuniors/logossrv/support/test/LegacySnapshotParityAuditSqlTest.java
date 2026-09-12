package com.josecjuniors.logossrv.support.test;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/** Validates the checked-in audit SQL against the real Flyway V41 test schema. */
@IntegrationTest
class LegacySnapshotParityAuditSqlTest {

    @Autowired
    private DataSource dataSource;

    private static final Path AUDIT = Path.of(
            "docs/audits/task-034b-legacy-snapshot-parity.sql");

    @Test
    void auditScriptExecutesReadOnlyAgainstV41Schema() throws Exception {
        Path auditPath = AUDIT.toAbsolutePath().normalize();
        assertThat(auditPath).exists();

        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.createStatement().execute("SET TRANSACTION READ ONLY");
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new FileSystemResource(auditPath.toFile())));
            connection.rollback();
        }
    }
}
