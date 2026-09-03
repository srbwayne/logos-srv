# ADR-0003 — External Subject Identity Is Namespaced and Decoupled from AppUser

## Status

Accepted and implemented by TASK-009R.

## Context

The current internal progression adapter accepts a `SubjectId` that is a compatibility bridge over `AppUserId`. Exposing that identifier to external consumers couples them to Logos persistence identity and does not support identities from more than one source.

## Decision

External consumers identify a subject with `namespace + externalId`. Logos stores an explicit mapping in `progression_subject_identity` and resolves it to the existing internal `SubjectId` before entering progression. The mapping is not authentication, does not auto-create players, and does not auto-link by email.

## Semantics

- `namespace` is trimmed, lowercased with `Locale.ROOT`, non-blank, and limited to 64 characters.
- `externalId` is trimmed, non-blank, limited to 255 characters, and case-sensitive.
- A player may have multiple external identities.
- `(namespace, externalId)` is unique.
- An absent mapping returns `NOT_FOUND` through the existing progression subject-not-found semantic.
- Existing players receive the `logos-native` mapping from their `AppUserId` string representation.

## Alternatives considered

- Reusing `conexao_externa`: rejected because that table represents integration connections and credentials.
- Making `SubjectId` itself external: rejected because internal progression state remains owned by Logos.
- Auto-linking by email or creating players during lookup: rejected because lookup must be explicit and deterministic.

## Consequences

Positive: consumers are decoupled from internal IDs, multiple origins can point to one player, and authentication remains a separate concern.

Negative: an additional table and mapping lifecycle are required; onboarding, namespace authorization, and service-to-service authentication remain future work.

## Migration/compatibility impact

`V32__Create_Progression_Subject_Identity_Table.sql` creates the table, constraints, FK, and `logos-native` backfill. The existing `POST /api/internal/v1/progression/{subjectId}/evaluate` endpoint and progression behavior are unchanged. No LifeOS mappings are created.

## Validation

`ExternalSubjectReference`, resolver, not-found, multiple identities, delegation, and PostgreSQL resolver tests cover the decision. V1–V32 and the backfill were validated against PostgreSQL 16.10 in isolated databases. The 37-test directed identity/progression set passed; the full suite retained only two pre-existing vicio errors.
