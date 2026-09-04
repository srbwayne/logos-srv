# TASK-010C — Define Configuration / SkillPolicy Compatibility Semantics

Status: `BLOCKED_FOR_HUMAN_DECISION`

Discovery de compatibilidade semântica. Não foram criados schema, entities,
resolvers, APIs, migrations ou alterações no engine.

## 1. Baseline

```text
branch: feat/lsrv-16-implementar-rotina-de-registro-de-vicios
HEAD: b432e8ab350b3306a642f3a9dd4476378de35f92
checkpoint: b432e8a docs: determine skill rules versioning scope
directed tests: 44 passed, 0 failures, 0 errors
```

O working tree já continha alterações não relacionadas em `compose.yaml`,
propriedades, código/testes de vícios e documentação. Elas foram preservadas.
Não houve alteração funcional após TASK-010D.

## 2. Current compatibility

```text
Current compatibility model: GLOBAL_CARTESIAN
```

Evidência: `JpaProgressionConfigurationResolver.toConfiguration` resolve
`AtividadeConfig` e, separadamente, executa `habilidadeRepository.findAll()`;
para cada habilidade projeta todas as suas regras em
`ProgressionConfiguration.skillBonusRules`. Não há filtro por
`AtividadeConfigId`, `ConfigurationVersion`, atividade ou jogador.

Assim, no modelo atual, a combinação efetiva é:

```text
qualquer AtividadeConfig atual
+
conjunto global atual de regras de habilidade
```

Não existem ainda `ConfigurationVersion` ou `SkillPolicyVersion` persistidos;
esta é a compatibilidade observada, não uma decisão de schema futuro.

## 3. Attribute interaction

Uma regra contém `habilidade_id`, `atributo_id` e `peso_distribuicao`. A
configuração possui seu próprio conjunto de distribuições por atributo.

O resolver carrega todas as skill rules, inclusive regras para atributos que a
configuração não usa. Depois, `ProgressionInputFactory` só transforma uma rule
em `ProgressionInput.SkillBonus` quando o `ProgressionProfile` possui a skill
correspondente. No engine, `SkillBonusCalculator` só calcula bônus para a
distribuição de atributo que está sendo iterada.

Consequentemente:

```text
rule para atributo fora da configuração:
carregada: YES
aplicada: NO
falha: NO
detalhe adicional: NO
```

Uma futura versão de configuração define apenas parcialmente seu universo de
atributos: ele é o conjunto de suas distribuições, mas não é usado como filtro
durante o carregamento da política.

## 4. Structural and semantic dependency

```text
SkillPolicy depends structurally on Configuration: NO
```

As tabelas atuais não possuem FK entre `regra_distribuicao_habilidade` e
`atividade_config`. A policy precisa de skill, atributo e peso; nenhum desses
campos referencia uma configuração específica.

```text
Semantic compatibility constraint exists: UNKNOWN
```

Não foram encontrados `categoria`, `tipo`, `contexto`, `grupo` ou outro
conceito que distinga policies por atividade. O código atual também não rejeita
combinações por incompatibilidade; apenas ignora, no cálculo, regras sem
distribuição de atributo correspondente. Isso prova o comportamento técnico,
mas não prova que qualquer combinação futura será válida no domínio.

## 5. Scenario analysis

| Cenário | Comportamento atual |
|---|---|
| Policy tem regra para atributo não usado pela configuração | `valid`; carregada, mas ignorada no cálculo |
| Configuração usa atributo sem skill rule | `valid`; não há bônus de skill |
| Policy remove regra existente | `valid`; resultado de atributo pode mudar |
| Policy adiciona regra para nova habilidade | `valid`; só influencia jogadores cujo perfil possui essa skill |

Nenhum cenário gera erro de compatibilidade no código atual. Remover/adicionar
ou alterar peso pode mudar o resultado quando a combinação skill/atributo entra
no perfil e na configuração.

## 6. Policy count

```text
Current evidence supports: ONE
```

Existe apenas um conjunto global observado, não uma entidade de policy nem
seleção de múltiplas policies. Suportar muitas policies é uma possibilidade
futura, não uma necessidade demonstrada pelo modelo atual.

## 7. Selection semantics

Para preservar o comportamento atual com o menor acoplamento, a seleção
recomendada para nova execução é:

```text
new execution
↓
resolve chosen/current ConfigurationVersion
↓
resolve current version of ONE global SkillPolicyDefinition
↓
freeze configurationVersionId + skillPolicyVersionId
↓
materialize ProgressionConfiguration
↓
execute
```

O caller externo não escolhe `SkillPolicyVersion`. Ele fornece somente a
referência de configuração e os fatos.

## 8. Policy rollout semantics

Recomendação para o modelo global:

```text
existing ConfigurationVersion uses new policy for new executions: YES
```

Se `S2` substituir `S1` como versão corrente, novas execuções de `C1` passam a
usar `S2`; uma execução já concluída permanece com o par congelado `C1 + S1`.

Isso é rollout automático da policy para novos eventos, não alteração de
histórico. Se o domínio exigir rollout explícito por configuração, a alternativa
de associação/pinning deve ser escolhida.

## 9. Model comparison

### A — Global current policy + frozen execution references

```text
ConfigurationVersion
        +
current global SkillPolicyVersion
        ↓
freeze both on execution
```

Preserva diretamente o comportamento atual. A policy nova afeta todas as
configurações em novas execuções, mas o par exato é persistido no evento.

### B — ConfigurationDefinition associated with SkillPolicyDefinition

```text
ConfigurationDefinition
        ↓
SkillPolicyDefinition
```

Cada definição de configuração escolhe uma família de policy; novas execuções
resolvem a versão corrente dessa policy e congelam ambas. Permite policies
distintas, mas introduz uma associação que não existe hoje.

### C — ConfigurationVersion pins SkillPolicyVersion

```text
ConfigurationVersion
        ↓
SkillPolicyVersion
```

A versão de configuração já nasce com uma policy específica. A combinação é
fortemente determinística, mas uma alteração de policy exige criar novas
configuration versions para as configurações que devem adotá-la.

## 10. Matrix

| Critério | Global Current | Definition Association | Version Pin |
|---|---:|---:|---:|
| Preserva comportamento atual | Alta | Média | Baixa/média |
| Deterministic replay | Alta | Alta | Alta |
| Independent policy rollout | Alta | Alta por grupo | Baixa |
| Config version fan-out | Baixo | Baixo | Alto |
| Runtime complexity | Baixa | Média | Baixa |
| Migration complexity | Baixa/média | Média/alta | Média |
| Operational simplicity | Alta | Média | Alta após criação |
| Auditability | Alta com par congelado | Alta com par congelado | Alta |
| External API simplicity | Alta | Alta | Alta |
| Future extensibility | Média | Alta | Média |

## 11. Policy activation and atomicity

```text
Atomic configuration-policy resolution required: YES
```

Mesmo no modelo global, não se deve fazer:

```text
resolve configuration
↓ policy current muda
↓ resolve policy
```

A resolução de ambos deve ocorrer dentro de uma operação transacional/snapshot
com regra clara de seleção. O par resultante deve ser congelado antes da
execução. `currentVersion` é mecanismo de seleção para evento novo, nunca fonte
de replay.

Uma referência corrente na definição ou uma flag equivalente pode indicar a
versão ativa; lifecycle completo não faz parte desta task.

## 12. Persisted combination

| Alternativa | Avaliação |
|---|---|
| Dois campos em `RegistroAtividade` | Mais simples e explícita para o estado atual; aumenta a tabela de evento com duas FKs. |
| `ExecutionConfiguration` | Melhor conceito se a combinação for reutilizada, auditada ou expandida com mais políticas; adiciona uma entidade/tabela. |
| Outro mecanismo | Não há mecanismo existente identificado. |

```text
ExecutionConfiguration is a useful domain concept: LATER
```

Para a primeira implementação, duas referências exatas podem ser suficientes.
Um `ExecutionConfiguration` persistido torna-se justificável quando houver mais
componentes versionados ou necessidade de identificar a composição como um
objeto próprio.

## 13. ProgressionConfiguration relation

O desenho futuro natural é:

```text
frozen configurationVersion + frozen skillPolicyVersion
↓
materialize complete ProgressionConfiguration
↓
ProgressionInput
↓
ProgressionEngine
```

Portanto:

```text
persisted identity: pair / ExecutionConfiguration
runtime value: immutable ProgressionConfiguration
```

`ProgressionConfiguration` não deve resolver `latest` nem acessar banco após
sua criação.

## 14. Replay contract

Replay deve carregar exatamente:

```text
facts
profile state at the relevant point
configurationVersionId
skillPolicyVersionId
```

`currentVersion` não pode participar da resolução de replay. Os registros
anteriores ao novo modelo continuam sem referências inventadas.

## 15. External API

```text
Caller chooses skill policy: NO
```

Todos os três modelos permitem que o caller envie apenas configuração e fatos.
No modelo A, Logos escolhe a policy global corrente; no B, segue a policy
associada à definição; no C, a policy já está pinada na versão.

A referência externa deve poder distinguir:

```text
logical configuration + current version for new event
logical configuration + exact version for explicit/replay operation
```

O contrato final não será definido nesta task.

## 16. Simplest correct model

Considerando comportamento atual, determinismo, rollout e API simples:

```text
ONE global SkillPolicyDefinition
↓
current SkillPolicyVersion for new events
↓
freeze configurationVersionId + skillPolicyVersionId per execution
```

Não há evidência de compatibilidade específica por configuração que justifique
uma associação obrigatória agora. O modelo deve, entretanto, permitir rejeitar
uma combinação no futuro se o domínio introduzir famílias de policy.

## 17. Migration consequences

Nenhuma migration foi criada. Conceitualmente, o modelo recomendado exigiria:

```text
progression_skill_policy
progression_skill_policy_version
progression_skill_policy_version_rule
progression_configuration_definition
progression_configuration_version
```

E, para persistir a composição, uma das opções:

```text
registro_atividade.configuration_version_id
registro_atividade.skill_policy_version_id
```

ou:

```text
execution_configuration
  configuration_version_id
  skill_policy_version_id
```

As migrations históricas não devem ser alteradas.

## 18. Backfill

O estado atual seria convertido conceitualmente em:

```text
ConfigurationVersion v1 por configuração atual
SkillPolicyVersion v1 com as regras globais atuais
```

Novas execuções futuras resolveriam o par corrente. Execuções antigas não
receberiam `S1` ou `C1` retroativamente sem evidência; permaneceriam sem versão
conhecida.

## 19. Remaining decisions

1. Confirmar que deve existir uma única `SkillPolicyDefinition` inicialmente.
2. Aprovar o rollout automático da policy corrente para novos eventos.
3. Confirmar a necessidade de validação de compatibilidade futura.
4. Escolher campos diretos ou `ExecutionConfiguration` para persistir o par.
5. Definir atomicidade/snapshot da resolução.
6. Definir o contrato futuro para current versus versão explícita.

## 20. Git and decision gate

```text
git diff --check: PASS
implementation: NOT STARTED
human decision required: YES
```

```text
TASK-010C DISCOVERY: COMPLETE
IMPLEMENTATION: NOT STARTED
HUMAN DECISION REQUIRED: YES
```
