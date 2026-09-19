# Architecture Tests

Potential future ArchUnit rules after packages stabilize:

- domain must not depend on `adapters`;
- domain must not depend on `application` commands/DTOs;
- engine must not depend on web/security/JPA adapters;
- adapters may depend inward through ports;
- LifeOS-specific transport types must not enter the core.

Do not add brittle package rules before the migration establishes the intended package boundaries.
