# Current Architecture

Status: **OBSERVED from the supplied repository baseline**.

## Technology

- Java 17
- Spring Boot 3.3.1
- Spring Web / Security / Data JPA
- PostgreSQL
- Flyway
- JJWT
- Spring Cloud Resilience4j dependency
- Spring REST Docs / Asciidoctor dependencies
- Awaitility in tests

## Structural style

The code already uses a pragmatic ports/adapters organization:

```text
com.josecjuniors.logossrv
├── adapters
│   ├── in/web
│   └── out/.../jpa
├── config
└── core
    └── <capability>
        ├── application
        └── domain
```

Capabilities observed include app user, scheduled/configured activities, activity form read model, attributes, debuffs, global stress, calculation factors, skills, player, activity/vice records and distribution/XP/stress rules.

## Important current coupling

`Jogador` is simultaneously a JPA entity and domain aggregate, owns personal profile fields, references `AppUser`, and imports `UpdatePerfilJogadorCommand` from the application layer. This is a concrete dependency-direction leak to address incrementally.

## Processing

`ProcessarRegistroAtividadeService` currently orchestrates activity-record processing and contains calculation logic for XP, stress, attribute distribution and skill bonus. It is triggered asynchronously from a transactional event and starts a new transaction.

## Persistence history

The repository contains Flyway migrations V1 through V31. Existing migrations are historical assets and must not be rewritten during modernization.

## Tests

The supplied tree contains controller/integration tests and a processing-service test. Coverage percentage is not established by this baseline.

## Operational gap

No canonical architecture/governance documentation was present before this docs package. CI/observability maturity must be inspected separately rather than assumed.
