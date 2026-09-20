# Reading progression POC

Status: POC / PRE-RELEASE.

The first canonical Reading POC attribute is **Conhecimento**. Reading and
study directly accumulate knowledge. `Inteligência` is intentionally not used:
it represents a broader, slower-moving human capability and is not modeled as
proportional to pages read.

The initial model is intentionally narrow:

- factor: `pages_read` (`NUMERICO`, unit `pages`)
- calculation mode: `FACT_VALUE`
- base XP: `1`
- Conhecimento distribution weight: `1.0`
- stress: `0`, with no stress rules

This produces one global XP and one Conhecimento XP for each reported page.
The ratio is experimental and is not a cognitive or scientific assertion.

The POC is authored through supported Logos APIs: it does not add a Flyway seed
or establish Reading data in every application database. Executions pin an
explicit published configuration revision for reproducibility.

The `lifeos` namespace may be used as simulated contract data in this Logos-side
contract/integration simulation. No LifeOS runtime participates. Future product
rules may distribute Reading across Conhecimento, Foco, or Disciplina when
supported by stronger evidence.
