# Progression Engine

The `ProgressionEngine` is the target deterministic core facade.

It should orchestrate pure or near-pure policies and produce a `ProgressionResult`. It must not own HTTP authentication or depend on LifeOS implementation classes.

First extraction candidate: calculation behavior currently concentrated in `ProcessarRegistroAtividadeService`.
