# PostgreSQL integration-test lifecycle

Use the `postgres-it` Maven profile for the PostgreSQL-backed test suite.
It creates a unique `logos_test_<run-id>` schema, lets Flyway apply the
canonical migrations, and drops the schema when the Spring test context closes.

Set these values in the runtime environment before running the suite:

```text
LOGOS_TEST_DB_URL=jdbc:postgresql://<host>:<port>/<disposable-database>
LOGOS_TEST_DB_USERNAME=<runtime-user>
LOGOS_TEST_DB_PASSWORD=<runtime-secret>
```

Then run:

```text
mvn test -Ppostgres-it
```

The lifecycle refuses the controlled manual E2E database
`logos_task010r_validation` and refuses to drop schemas whose names do not
match `logos_test_<32 hexadecimal characters>`.

`@IntegrationTest` keeps its existing transaction rollback. Tests that opt out
of that transaction, including committed/concurrency integration tests, still
run real transactions inside the disposable schema. The controlled E2E
database is not used by this Maven lifecycle.
