# TASK-010 — Configuration Identity & Versioning Discovery

Status: `BLOCKED_FOR_HUMAN_DECISION`

Esta é uma discovery arquitetural. Nenhuma entidade, migration, API, resolver,
referência atual ou regra de progressão foi alterada.

## 1. Baseline

```text
branch: feat/lsrv-16-implementar-rotina-de-registro-de-vicios
HEAD: 07ae7352d53848afda80b7f7a0701475519215f2
checkpoint: 07ae735 feat: expose external subject progression endpoint
directed tests: 44 passed, 0 failures, 0 errors
```

O working tree já continha alterações não relacionadas em `compose.yaml`,
propriedades, código/testes de vícios e documentação de governança. Elas foram
preservadas. A validação direcionada foi executada com PostgreSQL isolado e
Flyway V1–V32.

## 2. Current configuration model

Não existe uma entidade `Atividade` separada no modelo atual. A unidade usada
pela progressão é `AtividadeConfig` (`core/atividadeconfig/domain/model/AtividadeConfig.java`).

```text
AtividadeConfig
├── id: AtividadeConfigId -> UUID
├── nome: String
├── descricao: String
├── xpBase: Integer
├── estresseBase: Integer
├── diasParaPenalidade: Integer
├── xpPerdaPorCiclo: Integer
└── regrasDistribuicao: Set<RegraDistribuicaoAtividade>
    ├── atributo: Atributo
    ├── pesoPercentual: Double
    ├── regraFatorXPS: Set<RegraFatorXP>
    │   ├── fatorCalculo: FatorCalculo
    │   ├── pesoMultiplicador: Double
    │   ├── pontoCorteMin: Double
    │   └── pontoCorteMax: Double
    └── regraFatorEstresses: Set<RegraFatorEstresse>
        ├── pesoMultiplicador: Double
        ├── pontoCorteMin: Double
        ├── pontoCorteMax: Double
        └── tipo: TipoFatorEstresse
```

`ProgressionConfiguration` é um record transitório, não uma entidade. O
resolver copia `xpBase`, `estresseBase`, distribuições, regras de XP, regras de
stress e regras de bônus de habilidade para esse modelo
(`JpaProgressionConfigurationResolver.toConfiguration`).

Há uma dependência adicional: as regras de bônus de habilidade são obtidas por
`habilidadeRepository.findAll()` e todas as regras de distribuição de todas as
habilidades são projetadas como `SkillBonusRule`. Elas não possuem FK para
`AtividadeConfig`; portanto, hoje fazem parte efetiva do resultado resolvido,
mas não de uma configuração isolada.

`AtividadeFormulario` é uma projeção 1:1 de formulário (`versao` e
`data_geracao`), não uma versão das regras de progressão.

## 3. Persistence model

Mapa das tabelas relevantes após V1–V32:

```text
atividade_config (id UUID PK)
  1 ── N regra_distribuicao_atividade
                 (id UUID PK, atividade_config_id UUID FK, atributo_id UUID FK)
                 ├── 1 ── N regra_fator_xp
                 │          (id UUID PK, fator_calculo_id UUID FK)
                 └── 1 ── N regra_fator_estresse
                            (id UUID PK)

regra_fator_xp ── N:1 fator_calculo (id UUID PK)
habilidade ── 1:N regra_distribuicao_habilidade ── N:1 atributo
atividade_config ── 1:1 atividade_formulario (atividade_config_id UNIQUE)

registro_atividade
  (id UUID PK, jogador_id UUID FK, atividade_config_id UUID FK)
  └── 1:N registro_atividade_detalhe
              (fator_calculo_id UUID FK, valor_registrado VARCHAR(100))
```

Evidências: `V1__Initial_Schema.sql`, V5, V6, V9, V10, V14, V15, V17,
V18, V19, V20 e V22. V6 renomeou a relação de `atividade_id` para
`atividade_config_id`; V8 adicionou os campos de penalidade; V15 adicionou a
projeção de formulário.

Não há constraint de versão, revisão, vigência ou ponteiro `current` nas
tabelas atuais. Todas as chaves relacionadas usam UUID no Java e no PostgreSQL.

## 4. Current reference path

```text
ProgressionConfigurationReference(UUID value)
    ↓
JpaProgressionConfigurationResolver.resolve(...)
    ↓ new AtividadeConfigId(reference.value())
AtividadeConfigRepository.findById(AtividadeConfigId)
    ↓
AtividadeConfig + relações lazy de distribuição/regras
    + HabilidadeRepository.findAll() + regras de habilidade
    ↓
ProgressionConfiguration (snapshot transitório em memória)
```

O core conhece somente `ProgressionConfigurationReference(UUID)` e o port
`ProgressionConfigurationResolver`. O coupling com `AtividadeConfigId`, JPA e
o grafo legado está no adapter `JpaProgressionConfigurationResolver`. A
semântica, contudo, ainda é a do ID interno da entidade mutável.

## 5. Mutability

```text
Configuration mutable: YES
```

Evidências:

- `UpdateAtividadeConfigService.update` carrega uma configuração existente e
  chama `AtividadeConfig.atualizar`;
- podem mudar `nome`, `descricao`, `xpBase`, `estresseBase`,
  `diasParaPenalidade` e `xpPerdaPorCiclo`;
- `RegraDistribuicaoAtividade.atualizarPeso` possui caminho PUT de update;
- `RegraFatorXP.atualizar` permite trocar fator, multiplicador e cortes;
- `RegraFatorEstresse.atualizar` permite alterar multiplicador, cortes e tipo;
- `FatorCalculo.atualizar` permite alterar nome, unidade e tipo de input;
- existem services/controllers de create, update e delete para a configuração
  e suas regras.

Não há cópia automática das regras antes de uma alteração. `AtividadeConfig`
usa dirty checking JPA e `CascadeType.ALL/orphanRemoval` para sua coleção de
distribuições.

## 6. Historical versioning

```text
Versioning exists: NO
Historical versions retained: NO
```

`AtividadeFormulario.versao` é histórico da representação do formulário, não
da configuração usada no cálculo. Também não foram encontrados campos
`revision`, `effective_from`, `effective_to`, `active`, `history` ou mecanismo
de auditoria de regras.

Há delete físico por `DeleteAtividadeConfigService` e services de delete das
regras. Não há soft delete ou política de retenção para replay. As FKs não
definem `ON DELETE CASCADE`; a remoção de grafos com dependências deve ser
tratada pela ordem/cascata efetivamente aceita pelo JPA e pelo banco, mas não
preserva histórico.

## 7. Replay assessment

```text
Exact replay currently possible: ONLY IF CURRENT CONFIG DID NOT CHANGE
```

`RegistroAtividade` guarda `atividade_config_id`, fatos em
`registro_atividade_detalhe`, timestamp, jogador, `xp_ganho_final` e
`estresse_gerado`. Não guarda os valores das regras, um snapshot ou uma
versão. Portanto o resultado passado pode ser auditado como resultado
persistido, mas não recalculado com garantia a partir das regras históricas.

## 8. Execution linkage

```text
RegistroAtividade records exact configuration: PARTIALLY
```

Ele registra qual `AtividadeConfigId` foi associado ao evento e os resultados,
mas não qual estado do grafo de regras foi resolvido. `findByIdWithDetails`
carrega a configuração e suas regras atuais, não uma revisão histórica.

## 9. Consistency

```text
Resolver snapshot consistency: POTENTIALLY INCONSISTENT
```

O método é `@Transactional(readOnly = true)`, o que delimita a operação a uma
transação, mas o PostgreSQL usa normalmente `READ COMMITTED`. A configuração,
suas coleções lazy e as habilidades são obtidas por múltiplas consultas; uma
edição concorrente pode ser observada entre consultas. Além disso, o acesso às
coleções e às regras aninhadas pode produzir N+1. Não há hoje fetch join ou
snapshot versionado. Esta discovery não propõe otimização desse comportamento.

## 10. External identity assessment

| Opção | Avaliação |
|---|---|
| UUID interno (`AtividadeConfigId`) | Simples e compatível, mas expõe coupling e não identifica uma versão imutável. |
| Identidade externa namespaced | Útil se houver múltiplos donos/consumidores ou alias externo; sozinha não resolve versionamento. Pode ser complexidade adicional se o Logos for o único owner. |
| Chave lógica + versão (`configurationKey`, `revision`) | Expressa identidade estável e seleção exata; permite `latest` apenas no momento de criar um novo evento. Exige lifecycle e persistência de versões. |
| UUID por versão | Bom para referências opacas e imutabilidade; precisa ainda de identidade lógica para agrupar versões e resolver a versão atual. |

Conclusão: não basta renomear `AtividadeConfigId`. A referência pública deve
apontar para uma versão específica, enquanto a identidade lógica permite
descobrir a versão corrente para novos eventos.

## 11. Versioning alternatives

### Alternativa A — Definition + immutable versions (recomendada)

```text
ConfigurationDefinition (logical identity)
    1 ── N
ConfigurationVersion (integer revision, immutable rules)
```

- Schema impact: novas tabelas de definição/versão e relações versionadas para
  o grafo de regras; `RegistroAtividade` passa a referenciar a versão.
- Application impact: resolver separado para `latest` de evento novo e para
  revisão exata; alterações criam nova versão.
- Migration/backfill: cada `AtividadeConfig` atual vira versão 1; as regras
  atuais são copiadas sem mudar XP/estado.
- Historical replay: determinístico pela versão persistida.
- Pros: modelo explícito, auditável, simples de consultar e adequado a
  idempotência/replay.
- Cons: mais tabelas e lifecycle; edições deixam de ser updates in-place.
- Risks: decidir quando uma versão fica imutável e como tratar regras globais
  de habilidade hoje carregadas por `findAll()`.

### Alternativa B — Root mutável + revision history

Uma configuração lógica continua sendo editável, mas cada alteração grava uma
revisão completa e o evento referencia a revisão.

- Schema impact: tabelas de revisão/snapshot e metadados de alteração;
- Application impact: mutações precisam ser transacionais com a criação da
  revisão; resolver histórico precisa aceitar revisão explícita;
- Migration/backfill: estado atual vira a primeira revisão;
- Historical replay: possível se toda alteração gerar snapshot completo;
- Pros: preserva uma fachada mutável para administração e oferece auditoria;
- Cons: semântica mais complexa, maior risco de revisão incompleta e queries
  temporais mais difíceis;
- Risks: qualquer caminho de update fora do mecanismo de revisão quebra replay.

### Alternativa C — Snapshot completo no execution record

Mantém `AtividadeConfig` mutável e grava no `RegistroAtividade` todos os
valores resolvidos usados na execução.

- Schema impact: ampliação substancial do registro ou tabela de snapshots;
- Application impact: execução deve persistir o snapshot antes/de forma
  atômica com o resultado;
- Migration/backfill: registros antigos continuam sem snapshot exato;
- Historical replay: forte para execuções novas, inexistente para as antigas
  sem dados adicionais;
- Pros: replay local à execução e não exige congelar o CRUD de configuração;
- Cons: custo de storage, payload grande e ausência de uma identidade de
  configuração reutilizável por novos consumidores;
- Risks: divergência entre snapshots, schema pesado e maior complexidade de
  auditoria.

## 12. Comparison

| Critério | A: versões imutáveis | B: histórico de revisões | C: snapshot na execução |
|---|---|---|---|
| Reprodutibilidade | Alta | Alta, se completo | Alta para novos registros |
| Auditabilidade | Alta | Alta | Alta por execução |
| Complexidade | Média | Alta | Média/alta |
| Migration impact | Médio/alto | Médio/alto | Alto no registro |
| Storage cost | Médio | Médio/alto | Alto por execução |
| Query complexity | Baixa/média | Alta | Média |
| API stability | Alta | Média | Baixa para referência de config |
| LifeOS usability | Alta | Média | Baixa/média |
| Replay | Exato | Exato | Exato somente após adoção |
| Future idempotency | Forte | Forte | Forte por evento, sem config reutilizável |

## 13. Recommended architecture

Recomendação para decisão humana: Alternativa A, ainda não aprovada para
implementação.

```text
ConfigurationDefinition
├── stable logical identity
├── currentVersion pointer
└── versions 1:N
    ├── ConfigurationVersion 1 immutable
    ├── ConfigurationVersion 2 immutable
    └── ConfigurationVersion N immutable
        └── complete progression rules snapshot
```

Usar revisão inteira simples (`integer`) em vez de SemVer: a revisão representa
ordenação de alteração, não compatibilidade de software. UUID pode identificar
fisicamente uma versão, mas não substitui a revisão lógica. `effectiveFrom`/
`effectiveTo` só devem ser adicionados se houver agendamento ou retroatividade;
para a primeira versão, um ponteiro de versão corrente é suficiente.

## 14. Execution semantics

```text
new event
  ↓
resolve logical configuration's current version
  ↓
freeze exact ConfigurationVersionId/revision for the event
  ↓
execute deterministic progression
  ↓
persist result + exact version reference
```

Para replay, nunca resolver implicitamente `latest`. `latest` é permitido
somente na criação de um novo evento; registros existentes devem apontar para
uma versão específica. A combinação “mesmo evento + mesma versão imutável”
fornece a base para idempotência futura, sem implementá-la nesta task.

## 15. Migration impact

Nenhuma migration foi criada. Em uma futura implementação, seriam
potencialmente necessárias:

- tabela de identidade lógica da configuração;
- tabela de versões imutáveis;
- tabelas/colunas para snapshot das distribuições, regras XP/stress e escopo
  das regras de bônus de habilidade;
- FK de `registro_atividade` para a versão efetivamente usada;
- ponte temporária compatível com `atividade_config_id` durante a transição.

As migrations V1–V32 não devem ser modificadas.

## 16. Backfill strategy

Sem executar alteração, o backfill conceitual seria:

1. para cada `AtividadeConfig`, criar uma `ConfigurationDefinition`;
2. copiar os campos base e o grafo atual para a versão inteira 1;
3. definir essa versão como corrente;
4. preservar `atividade_config_id` como bridge durante a transição;
5. não reprocessar nem alterar XP, nível, stress ou atributos dos jogadores.

Registros de atividade antigos não podem receber retroativamente uma versão
exata sem evidência histórica adicional; devem ser classificados como legados
ou vinculados à versão 1 somente com uma decisão explícita sobre essa hipótese.

## 17. API impact

- `ProgressionConfigurationReference(UUID)` permanece bridge interno até a
  aprovação do novo contrato;
- o futuro request externo deve carregar referência lógica e versão explícita,
  ou uma referência de versão opaca equivalente;
- o consumidor, incluindo LifeOS, não deve conhecer `AtividadeConfigId`, JPA ou
  o grafo de tabelas;
- nenhum endpoint de configuração ou contrato HTTP foi criado nesta task.

## 18. Risks

- ambiguidade histórica dos registros já processados;
- configuração mutável e regras globais de habilidade fora do aggregate;
- replay inválido após update/delete;
- exclusão física sem retenção histórica;
- edição concorrente entre consultas do resolver;
- migração de regras atuais sem omitir dependências;
- compatibilidade temporária entre `AtividadeConfigId` e versão nova;
- necessidade de decidir ownership e lifecycle de definições/versões.

## 19. Decision required

```text
TASK-010 DISCOVERY: COMPLETE

IMPLEMENTATION: NOT STARTED

HUMAN DECISION REQUIRED: YES
```

Decisões solicitadas:

1. aprovar ou rejeitar `ConfigurationDefinition 1:N immutable ConfigurationVersion`;
2. escolher a identidade pública: chave lógica + revisão, UUID por versão ou
   outra forma explicitamente aprovada;
3. definir se regras de habilidade são parte da versão e qual é seu escopo;
4. definir lifecycle (`DRAFT`, `ACTIVE`, `RETIRED` ou equivalente) e o momento
   de congelamento;
5. decidir o tratamento de `RegistroAtividade` histórico sem versão;
6. decidir se vigência temporal é necessária;
7. aprovar a estratégia de backfill e a duração da bridge
   `AtividadeConfigId`.

Nenhuma implementação deve começar antes dessa aprovação.
