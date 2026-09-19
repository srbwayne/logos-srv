# Product Vision

## DECIDED direction

Logos evolves from a standalone routine-gamification backend into an **independent deterministic progression engine/service**.

Its first intended client is LifeOS, but Logos must not become a LifeOS module or require LifeOS internals to execute its domain rules.

## Core responsibility

Given an identified subject, a progression profile, configured rules and a behavioral/activity fact, Logos determines progression consequences such as:

- XP;
- levels;
- attributes;
- skills and skill points;
- deterministic bonuses;
- gamified stress;
- vices, relapses and debuffs where those concepts remain part of the approved domain.

## Not the responsibility of the future Logos core

- login/password lifecycle;
- personal identity profile;
- CPF, address and other personal profile data;
- LifeOS health/book/habit source-of-truth data;
- cognitive recommendations (Noema responsibility);
- generic integration-token ownership unless explicitly retained by a future ADR.

## Product principle

LifeOS records **what happened**. Logos calculates **what that fact means for progression**. Noema may reason about **what should be done next**.
