# Historical Pre-Release Contract — External Subject Progression HTTP

This document records the superseded non-durable pre-release adapter. It is
retained as history; it is not a first-supported Logos HTTP contract.

```http
POST /api/internal/v1/progression/external/{namespace}/{externalId}/evaluate
```

The authenticated caller supplies only the existing `ProgressionEvaluationRequest`:

```json
{
  "configurationId": "<uuid>",
  "details": [{"factorKey": "<key>", "value": 1.0}]
}
```

The adapter constructs `ExternalSubjectReference`, resolves the explicit mapping to the internal `SubjectId`, and delegates to configured progression. The existing `ProgressionEvaluationResponse` is returned with `result` and `profile`.

`namespace` is trimmed and lowercased. `externalId` is trimmed, opaque, and case-sensitive; it is not parsed as UUID. Since this V1 representation uses path variables, `/` is not supported inside `externalId`.

The endpoint inherits the current authentication requirement. Missing mappings and configurations return `404`; invalid namespace, external ID, or request bodies return `400`. The compatibility endpoint remains available:

```http
POST /api/internal/v1/progression/{subjectId}/evaluate
```
