# External Progression Configuration HTTP Contract

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

The current V1 routes remain available and continue to accept their UUID
compatibility reference. V2 callers do not send `AtividadeConfigId`, JPA
entities, progression state, rules or skill policy references. Logos resolves
the current global skill policy for a new execution. An explicit configuration
revision is not replay; replay will use the references persisted with the
historical execution.

Invalid key/revision input returns `400`; missing configuration or subject
mapping returns `404`. Both V1 and V2 retain the existing authentication
protection. The response shape is the existing `result` plus `profile`; no
internal UUID is exposed.
