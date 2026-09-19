# Migration Baseline

Baseline inspected from the supplied repository copy.

- Branch: `feat/lsrv-16-implementar-rotina-de-registro-de-vicios`
- HEAD: `9c7ffa65056bdef771968ba074b0ef5ceada350b`
- Latest commit: `feat-LSRV-16: Implementa rotina completa de registro de vícios`
- Java: 17
- Spring Boot: 3.3.1
- Flyway migrations: V1 through V31
- Test source files observed: 20

## Important repository condition

The supplied working tree already contains a large number of modified files unrelated to this documentation package. Agents must treat them as pre-existing and must not normalize/revert them without explicit instruction.

## High-value baseline behavior

- activity record creation and asynchronous processing;
- XP and stress factor calculation;
- attribute XP distribution;
- skill-derived bonus;
- global player progression;
- skill unlock/points;
- vice/debuff flows;
- dynamic activity-form/read-model capability.

## Known architectural debt to characterize

- domain model coupled to JPA;
- `Jogador` coupled to `AppUser` and personal profile data;
- domain `Jogador` importing application command;
- progression arithmetic mixed with orchestration;
- asynchronous in-memory event delivery durability;
- implicit numeric conversion/rounding semantics.
