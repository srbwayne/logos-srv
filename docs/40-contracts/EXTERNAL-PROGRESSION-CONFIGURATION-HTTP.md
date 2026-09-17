# Historical Pre-Release Contract — External Progression Configuration HTTP

This document records the superseded V2 non-durable pre-release adapter. It
is retained as history; it is not a first-supported Logos HTTP contract.

The V2 progression boundary accepts a stable configuration key and an
optional positive integer revision. The key is trimmed and normalized to
lowercase with `Locale.ROOT`; revision `null` resolves the definition's
current version, while an explicit revision resolves that exact immutable
version. A syntactically valid but unknown key or revision returns `404`.

## Routes

```http
POST /api/internal/v2/progression/{subjectId}/evaluate
POST /api/internal/v2/progression/external/{namespace}/{externalId}/evaluate
```

Example request:

```json
{
  "configuration": { "key": "daily-reading", "revision": 3 },
  "details": [{ "factorKey": "minutes", "value": 30 }]
}
```

During this pre-release V2 experiment, the earlier V1 evaluate routes were
still available and accepted their UUID compatibility reference. V2 callers
did not send `AtividadeConfigId`, JPA entities, progression state, rules or
skill policy references. Logos resolved the global skill policy for a new
execution. An explicit configuration revision was not replay; replay used
the references persisted with the historical execution.

At that time, invalid key/revision input returned `400`; missing
configuration or subject mapping returned `404`. The experimental V1 and V2
routes used the existing authentication protection. The response shape was
the existing `result` plus `profile`; no internal UUID was exposed.

The V1/V2 evaluate routes described here were superseded before the first
supported Logos progression HTTP contract and are no longer mapped after
TASK-041.
