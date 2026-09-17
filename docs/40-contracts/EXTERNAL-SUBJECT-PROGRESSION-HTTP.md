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

At the time this pre-release adapter was active, the endpoint inherited the
existing authentication requirement. Missing mappings and configurations
returned `404`; invalid namespace, external ID, or request bodies returned
`400`. The same pre-release generation also exposed the following
compatibility route:

```http
POST /api/internal/v1/progression/{subjectId}/evaluate
```

These evaluate routes were superseded before the first supported Logos
progression HTTP contract and are no longer mapped after TASK-041.
