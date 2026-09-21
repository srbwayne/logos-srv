ALTER TABLE atributo
    ADD COLUMN semantic_key VARCHAR(64);

CREATE UNIQUE INDEX uq_atributo_semantic_key
    ON atributo (semantic_key)
    WHERE semantic_key IS NOT NULL;
