# TASK-003 — Decouple Progression Engine Input from RegistroAtividade

Status: **done**

- Baseline: `185916d`; behavioral checkpoint: `44df0a3`
- Completed: 2026-09-02
- Outcome: `ProgressionInput` tornou-se a fronteira explícita do engine; o mapping permanece no serviço de aplicação.
- Validation: 14 testes direcionados aprovados, incluindo testes diretos sem Spring/JPA e integração de processamento.
- Known limitations: o mapping ainda conhece entidades JPA; `NivelXPService` continua externo; vícios e configuração permanecem fora do escopo.
