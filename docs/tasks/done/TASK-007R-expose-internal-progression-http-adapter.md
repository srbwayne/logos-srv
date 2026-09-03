# TASK-007R - Expose Internal Progression HTTP Adapter

Status: done

Baseline: `6e893e1`.

## Outcome

Created `POST /api/internal/v1/progression/{subjectId}/evaluate`. The request
contains only `configurationId` and fact details; the subject is supplied in
the path. The controller maps these values to `SubjectId`,
`ProgressionConfigurationReference`, and `ProgressionFact`, then delegates to
`ExecuteConfiguredSubjectProgressionUseCase`.

The explicit response separates the execution result from the canonical
updated profile. No JPA entities or progression rules are exposed.

The route remains protected by the existing `anyRequest().authenticated()`
security policy. Missing subjects/configurations map to HTTP 404 through the
existing global advice; malformed UUIDs map to HTTP 400 through Spring MVC.

## Validation

Five controller tests pass, including happy path, ownership shape, missing
subject, missing configuration, and invalid UUID. The previous 24 targeted
progression tests and the Spring processing integration test also pass. No
schema, authentication, LifeOS, or existing API behavior changed.
