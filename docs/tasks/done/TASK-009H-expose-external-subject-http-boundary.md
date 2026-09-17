# TASK-009H — Expose External Subject HTTP Boundary

Status: COMPLETE — HISTORICAL PRE-RELEASE CONTRACT

This task was completed at the time it was implemented.

The evaluate HTTP boundary documented below was later superseded and removed
by TASK-041 when Logos established its first supported progression HTTP
contract:

```http
/api/internal/v1/progression/executions
```

## Resultado histórico

Foi exposta a fronteira autenticada:

```http
POST /api/internal/v1/progression/external/{namespace}/{externalId}/evaluate
```

O controller constrói `ExternalSubjectReference` e delega ao fluxo existente:

```text
HTTP → ExternalSubjectReference → ExternalSubjectResolver → SubjectId
     → ExecuteConfiguredSubjectProgressionUseCase → response existente
```

O endpoint legado `POST /api/internal/v1/progression/{subjectId}/evaluate` foi preservado.

Essas rotas de evaluate eram uma fronteira HTTP de pré-lançamento e foram
substituídas antes do primeiro contrato HTTP de progressão Logos suportado.
Não estão mais mapeadas após TASK-041.
Não houve alteração de migration, schema, autenticação, configuração ou regras do core de progressão.

## Semantics e erros

- `namespace`: trim e lowercase no VO;
- `externalId`: trim, opaco e case-sensitive;
- mapping ausente: `404 Not Found`;
- namespace/externalId/body inválidos: `400 Bad Request`;
- chamada sem autenticação: `403 Forbidden`, comportamento atual da security configuration;
- `externalId` contendo `/` permanece limitação do transporte path, não do VO.

## Validação

- PostgreSQL 16.10 isolado com Flyway em V32: sucesso;
- integração real HTTP/JPA: 3 testes verdes;
- suíte direcionada: 44 testes verdes;
- full suite: 165 executados, 163 verdes e 2 erros preexistentes em `RegistroVicioControllerTest`.

Documentação da API: `docs/40-contracts/API.md`.
