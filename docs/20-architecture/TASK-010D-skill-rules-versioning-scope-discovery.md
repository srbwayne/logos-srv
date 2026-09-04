# TASK-010D — Determine Skill Rules Versioning Scope

Status: `BLOCKED_FOR_HUMAN_DECISION`

Discovery de domínio/arquitetura. Não foram alterados migrations, entidades,
resolvers, `ProgressionConfiguration`, engine, API ou regras de produção.

## 1. Baseline

```text
branch: feat/lsrv-16-implementar-rotina-de-registro-de-vicios
HEAD: a781620ed5c219e8f53c4575d238d13f19f73f74
checkpoint: a781620 docs: record configuration identity versioning discovery
directed tests: 44 passed, 0 failures, 0 errors
```

O working tree já continha alterações preexistentes em `compose.yaml`,
configuração, código/testes de vícios e documentação. Elas foram preservadas.
O baseline foi executado com PostgreSQL isolado e Flyway V1–V32.

## 2. Skill model

O modelo distingue `Habilidade` da regra que distribui o bônus dessa habilidade:

```text
habilidade (id UUID PK)
  1:N regra_distribuicao_habilidade
          (id UUID PK,
           habilidade_id UUID FK,
           atributo_id UUID FK,
           peso_distribuicao DOUBLE PRECISION NOT NULL)
```

Evidências: `Habilidade.java`, `RegraDistribuicaoHabilidade.java` e
`V5__Refactor_RegraDistribuicaoHabilidade.sql`. Uma habilidade pode ter zero ou
várias regras; cada regra aponta para uma habilidade e um atributo. Não existe
FK para `atividade_config`, `jogador` ou `habilidade_jogador`.

O estado do jogador é separado:

```text
habilidade_jogador
  jogador_id UUID FK
  habilidade_id UUID FK
  nivel_atual INT
```

Evidência: `HabilidadeJogador.java`. O estado possui o nível atual do jogador e
não contém a regra de distribuição.

## 3. Rule semantics

```text
RegraDistribuicaoHabilidade.pesoDistribuicao
↓ JpaProgressionConfigurationResolver
ProgressionConfiguration.SkillBonusRule(
    skillKey, attributeKey, distributionWeight
)
↓ ProgressionInputFactory
ProgressionInput.SkillBonus(attributeKey, distributionWeight, skillLevel)
↓ SkillBonusCalculator
attribute XP bonus = distributionWeight * skillLevel * 0.05
```

Evidências: `JpaProgressionConfigurationResolver.toConfiguration`,
`ProgressionInputFactory.create` e `SkillBonusCalculator.calculate`.

A regra influencia diretamente o XP de atributo. Não altera diretamente XP
global, stress ou nível da habilidade. O XP de atributo pode alterar
indiretamente nível de atributo e pontos de habilidade em
`ProgressionProfile.apply`.

## 4. Player state vs rule

```text
SkillRule  = como uma habilidade contribui para XP de um atributo
SkillState = nível atual daquela habilidade para um jogador
```

`ProgressionProfile.SkillState(key, level)` vem do perfil carregado. A regra
fornece o peso; o estado fornece o nível. A futura versão deve congelar a
regra, nunca o estado do jogador, que pertence ao `ProgressionProfile`.

## 5. Current resolution path

```text
ProgressionConfigurationReference(UUID)
↓ JpaProgressionConfigurationResolver.resolve
↓ AtividadeConfigRepository.findById
↓ AtividadeConfig.regrasDistribuicao
  ├── regras XP/stress
  └── HabilidadeRepository.findAll()
      ↓ Habilidade.regrasDistribuicao
↓ ProgressionConfiguration.skillBonusRules
↓ ProgressionInputFactory + ProgressionProfile.skills
↓ ProgressionInput.skillBonuses
↓ ProgressionEngine
```

O engine não acessa banco ou repository. Ele recebe `ProgressionInput` e
`ProgressionProfile`; o `SkillBonusCalculator` opera sobre dados já
materializados.

## 6. Global scope

```text
Skill rules truly global: PARTIALLY
Direct configuration relation: NO
Direct player relation: NO
```

Persistência e seleção são globais: a regra liga apenas `Habilidade` e
`Atributo`, e o resolver carrega todas as habilidades para toda configuração.
Entretanto, “global” não é uma `SkillPolicy` explicitamente modelada; é uma
consequência da implementação atual. Podem existir várias regras por habilidade
e não há seleção por configuração.

## 7. Mutability and deletion

```text
Skill rules mutable: YES
create: YES
update: YES
delete: YES
Physical delete supported: YES
```

Evidências: `CreateRegraDistribuicaoHabilidadeService`,
`UpdateRegraDistribuicaoHabilidadeService`,
`DeleteRegraDistribuicaoHabilidadeService` e
`RegraDistribuicaoHabilidadeController` (POST/PUT/DELETE em
`/api/habilidades/{habilidadeId}/regras-distribuicao`).

O único campo próprio mutável é `pesoDistribuicao`. Não há soft delete,
snapshot, auditoria ou retenção histórica. `Habilidade` usa
`CascadeType.ALL/orphanRemoval` para suas regras.

## 8. Historical preservation

```text
Skill rule versioning exists: NO
Historical versions retained: NO
```

Não há revisão, vigência ou histórico real em entidade, tabela ou migration.
Alterar/excluir a regra atual remove a possibilidade de reconstruir sua versão
anterior.

## 9. Determinism test

A análise estática é suficiente para a caracterização; não foi criado teste
permanente. Cenário baseado nos métodos reais:

```text
Configuration C: baseXp=100, distribution(A, weight=.5),
                 xpRule(F, multiplier=1, cutoff contendo 100)
Fact F=100
Player P: SkillState(S, level=10)
S1: rule(S, A, peso=10)
S2: mesma rule, peso=20
```

`XpCalculator` e `ProgressionEngine` produzem XP da distribuição igual a 50.
`SkillBonusCalculator` produz:

```text
S1: 10 * 10 * .05 = 5  → attribute XP = 55
S2: 20 * 10 * .05 = 10 → attribute XP = 60
```

Logo:

```text
same configuration + same facts + changed skill rule = different result
```

O `xpGlobal` e o stress permanecem iguais nesse exemplo, mas
`ProgressionResult.attributeProgressions` muda; o perfil pode também atingir
níveis/pontos diferentes dependendo dos limiares.

## 10. Replay

```text
Replay affected by skill rule changes: YES
Classification: CONFIRMED
```

O mesmo `AtividadeConfigId` pode resolver S1 em T1 e S2 em T2, porque a regra é
carregada por `findAll()` no instante da resolução.

## 11. Snapshot consistency

```text
Skill-rule snapshot: POTENTIALLY INCONSISTENT
```

`JpaProgressionConfigurationResolver.resolve` é `@Transactional(readOnly=true)`,
mas carrega `AtividadeConfig` e habilidades por consultas distintas. Com
PostgreSQL `READ COMMITTED`, elas podem observar momentos diferentes. As
coleções lazy aninhadas também são acessadas progressivamente e podem produzir
N+1. Não existe isolamento repeatable-read nem snapshot persistido.

## 12. ProgressionConfiguration completeness

```text
ProgressionConfiguration self-contained deterministic snapshot: PARTIALLY
DB lookup inside engine: NO
```

Depois de materializado, o record contém os valores usados pelo engine,
incluindo `skillBonusRules`; nenhum lookup posterior ocorre. Porém o record é
transitório e depende de regras globais mutáveis no momento da resolução.

## 13. Ownership conclusion

```text
Current semantic classification: GLOBAL POLICY
```

A evidência é a ausência de relação com configuração/jogador e a resolução de
todas as habilidades para qualquer configuração. Esta classificação deve ser
confirmada pelo domínio, pois “global” pode ser apenas um acidente da
implementação atual.

## 14. Alternatives

### A — Embed in `ConfigurationVersion`

```text
ConfigurationVersion
├── XP rules
├── stress rules
├── distributions
└── skill bonus rules snapshot
```

Determinismo e replay são fortes, com uma referência única e API simples. A
migração copiaria as regras atuais para a versão 1 de cada configuração. Porém
uma mudança global usada por 100 configurações poderia exigir 100 novas versões;
há duplicação e alto fan-out.

### B — `SkillPolicyDefinition` + `SkillPolicyVersion`

```text
ConfigurationVersion + SkillPolicyVersion
```

Uma política global v2 pode ser reutilizada pelas próximas execuções sem criar
100 configurações novas. Exige congelar e persistir ambas as referências e
proibir a combinação implícita com `latest`. Traz melhor alinhamento semântico,
mas aumenta a complexidade de compatibilidade entre versões.

### C — Snapshot resolvido dentro de `ConfigurationVersion`

Ao criar uma versão de configuração, copia-se o resultado das regras globais
para dentro dela. Evita lookup mutável em runtime e mantém uma referência única,
mas é semanticamente próximo do embed e pode esconder a dependência transversal.
Mudanças globais não alcançam versões existentes implicitamente.

## 15. Comparison

| Critério | Embed | Separate Policy | Snapshot |
|---|---:|---:|---:|
| Deterministic replay | Alta | Alta | Alta |
| Domain clarity | Média/alta | Alta se global | Média |
| Storage | Alto | Menor | Alto |
| Migration complexity | Média | Média | Média |
| Runtime complexity | Baixa | Média/alta | Baixa |
| Update fan-out | Alto | Baixo | Baixo |
| Operational simplicity | Alta | Média | Alta |
| External API simplicity | Alta | Média | Alta |
| Auditability | Alta | Alta | Alta |
| Coupling | Baixo em runtime | Coordenação entre refs | Baixo em runtime |

## 16. Recommended option

```text
RECOMMENDED OPTION: B — SkillPolicyDefinition + SkillPolicyVersion
```

A recomendação segue a semântica transversal observada e evita recriar versões
de todas as configurações em cada alteração global.

A unidade congelada deveria ser conceitualmente:

```text
ExecutionConfiguration
├── configurationVersionId
└── skillPolicyVersionId
```

Não é aceitável `ConfigurationVersion immutable + latest global skill rules`.
Alternativa A deve ser escolhida se a decisão de domínio confirmar que a
globalidade atual é acidental e cada regra deve pertencer à configuração.

## 17. Migration consequences and backfill

Nenhuma migration foi criada. A alternativa B poderia exigir:

```text
progression_skill_policy
progression_skill_policy_version
progression_skill_policy_version_rule
progression_configuration_definition
progression_configuration_version
execution_configuration
```

O backfill conceitual criaria uma política global v1 com todas as regras atuais;
cada configuração atual originaria sua versão 1 e apontaria para essa política.
Isso não atribui retroativamente v1 a execuções antigas: elas permanecem sem
versão conhecida.

## 18. Execution and API semantics

```text
new event
↓ resolve ConfigurationVersion current
↓ resolve compatible SkillPolicyVersion current
↓ freeze both exact references
↓ build self-contained ProgressionConfiguration
↓ execute with ProgressionInput + ProgressionProfile
↓ persist exact references
```

O caller idealmente não conhece `SkillPolicyVersion`; fornece configuração e
fatos, enquanto Logos resolve e congela a política. Nenhuma API foi alterada.

## 19. Remaining decisions

1. Confirmar se `GLOBAL POLICY` é semântica de domínio ou acidente atual.
2. Aprovar B ou escolher A/C.
3. Definir escopo das habilidades/atributos de uma política.
4. Definir compatibilidade entre `ConfigurationVersion` e `SkillPolicyVersion`.
5. Definir se a execução terá tabela composta ou duas FKs.
6. Definir lifecycle e congelamento da política.
7. Aprovar o backfill sem inventar versões históricas.

## 20. Git and decision gate

```text
git diff --check: PASS
implementation: NOT STARTED
human decision required: YES
```

```text
TASK-010D DISCOVERY: COMPLETE
IMPLEMENTATION: NOT STARTED
HUMAN DECISION REQUIRED: YES
```
