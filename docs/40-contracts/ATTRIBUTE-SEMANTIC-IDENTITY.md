# Attribute semantic identity

`Atributo.id` is the internal persistent UUID. `Atributo.nome` is the mutable,
human-readable domain name. `Atributo.semanticKey` is an explicit,
machine-facing identity for integrations.

Semantic keys are nullable during the legacy transition, unique when present,
normalized as lowercase ASCII `snake_case` (1–64 characters), and immutable
once assigned. They are never derived from `nome`; changing `nome` does not
change the semantic identity. The key may be assigned at creation or through
the explicit `/api/atributos/{id}/semantic-key` operation.

Existing Progression HTTP V1 `key` fields remain Attribute UUIDs. This slice
adds catalog identity without changing published progression snapshots, skill
policy snapshots, engine/profile identity, or durable execution serialization.

For durable progression executions, `semanticKey` is additive response metadata
captured at execution time. New POST results, idempotent replay, exact reads,
and history reads use the persisted snapshot rather than resolving the current
Attribute catalog. Legacy executions without a snapshot expose `semanticKey`
as `null`.
