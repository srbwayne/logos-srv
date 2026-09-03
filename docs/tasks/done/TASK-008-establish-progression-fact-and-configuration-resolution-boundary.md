# TASK-008 - Establish Progression Fact and Configuration Resolution Boundary

Status: done

Baseline: `2359842`.

## Outcome

Created explicit `ProgressionFact`, `ProgressionConfiguration`, and
`ProgressionConfigurationReference` models. Configuration is resolved through
`ProgressionConfigurationResolver` and adapted from the existing
`AtividadeConfig` persistence model. `ProgressionInputFactory` combines facts,
configuration, and state-dependent skill levels without exposing rules to a
caller.

`ConfiguredStatefulProgressionApplicationService` composes configuration
resolution, profile loading, the existing stateful progression service, and
profile persistence. No HTTP adapter was added; TASK-007 is now unblocked at
the model boundary but remains a separate implementation task.

## Validation

Twenty-four targeted tests pass, including factory composition, configuration
resolution orchestration, subject/stateful tests, engine/profile tests,
characterization tests, and persistence integration tests. No migration, API,
authentication, LifeOS, event, or vicio behavior was changed.
