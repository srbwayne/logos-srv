# API Contract Direction

A future internal progression endpoint may conceptually evaluate:

```text
subjectId + fact + metrics + context -> ProgressionResult
```

The concrete JSON schema, endpoint name and version are intentionally not frozen here. They must be derived from one real LifeOS pilot vertical and the extracted engine contract.

The API should expose enough explanation to make deterministic results auditable (for example, applied rules and component deltas) without leaking persistence entities.
