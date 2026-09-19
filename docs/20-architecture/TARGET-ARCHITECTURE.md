# Target Architecture

Status: **DECIDED direction; physical module boundaries remain PROPOSED until proven by migration**.

## Dependency direction

```text
                 adapters/in
                     |
                     v
                 application
                 /         \
                v           v
             domain <---- engine
                ^           ^
                |           |
             ports/out      |
                ^           |
                |           |
             adapters/out---+
```

The domain/engine must not depend on HTTP, JWT, LifeOS, Spring MVC or database implementation details.

## Logical target

```text
logos
├── domain
│   ├── progression
│   ├── attribute
│   ├── skill
│   └── rule
├── engine
│   ├── xp
│   ├── stress
│   ├── distribution
│   └── progression
├── application
│   ├── port/in
│   ├── port/out
│   └── service
└── adapters
    ├── in/web
    └── out/persistence
```

Do not create Maven modules merely to match this diagram. First prove these boundaries inside the existing application.

## Identity boundary

The future progression state references an external subject identifier. Logos does not need a duplicate personal profile to calculate progression.

## Persistence

Logos owns its progression state and rule configuration. LifeOS owns its own facts and personal records. No cross-service JPA relationships or shared tables.
