# XP

## OBSERVED

XP calculation currently uses base XP, configured factor rules, multipliers and percentage weights. Processing also applies skill-derived bonus when distributing XP to attributes.

## Migration requirement

Before extraction, tests must characterize boundary values, null cutoffs, multiple factors, missing factors, negative/zero values where accepted, and conversion from floating-point calculations to integer XP.

Do not change rounding/truncation semantics accidentally during refactoring.
