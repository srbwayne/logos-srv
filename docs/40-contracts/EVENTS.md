# Events

## Current

The repository uses Spring transactional events for asynchronous processing, including activity-record processing/read-model workflows.

## Target principles

- distinguish domain event from transport event;
- event handlers that mutate progression must be idempotent before durable asynchronous delivery is introduced;
- in-memory Spring events are not assumed to provide durable delivery;
- do not introduce a message broker until delivery requirements justify it.
