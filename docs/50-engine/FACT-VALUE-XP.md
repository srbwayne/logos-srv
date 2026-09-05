# Fact-value XP calculation

XP rules have explicit calculation modes in the versioned snapshot:

* `FIXED`: `baseXp * multiplier * distributionWeight`;
* `FACT_VALUE`: the same value multiplied by the matching numeric fact.

The fact value remains subject to the existing inclusive cutoff checks. The
existing unmatched-cutoff division behavior is preserved before applying the
fact value. Final conversion remains `Double.longValue()`.

All V33 rules are explicitly materialized as `FIXED` by V35. Legacy mutable
rules also materialize as `FIXED`; the legacy model does not infer behavior
from `TipoInput.NUMERICO`.

`FACT_VALUE` requires the configured factor to be present and rejects negative
values before the progression result is applied. Zero is valid and produces
zero XP. Decimal values remain valid and use the existing truncation semantics.

`reading@1` remains unchanged as `FIXED`. The controlled validation database
contains `reading@2` as `FACT_VALUE`, with `pages_read`, `LEARNING`, base XP 1,
and weight 1.0. This bootstrap data is not a migration and is not applied to
 the production schema by V35.

The PostgreSQL mapping for `Atributo.descricao` is a regular nullable
`String` mapped to the existing `TEXT` column. It intentionally does not use
`@Lob`, because Hibernate 6 otherwise requests a PostgreSQL large object
(`Clob`) and fails while loading ordinary text descriptions.
