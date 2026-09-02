# TASK-002 — Extract Progression Calculation Model

Status: **done**

- Baseline: `44df0a3`
- Completed: 2026-09-02
- Implementation commit: see the TASK-002 refactoring commit in Git history
- Outcome: XP, estresse, bônus de habilidade e composição de progressão foram extraídos para componentes internos testáveis. `NivelXPService` permaneceu responsável pela progressão de nível.
- Validation: testes de caracterização e integração de processamento aprovados.
- Known out-of-scope failures: `RegistroVicioControllerTest` e cópia de `application.properties`, conforme baseline.
