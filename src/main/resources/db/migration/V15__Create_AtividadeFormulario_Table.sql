-- V15__Create_AtividadeFormulario_Table.sql

CREATE TABLE atividade_formulario (
    id UUID PRIMARY KEY,
    atividade_config_id UUID NOT NULL UNIQUE,
    formulario_json JSONB NOT NULL,
    versao INT NOT NULL,
    data_geracao TIMESTAMP NOT NULL,
    CONSTRAINT fk_formulario_atividade_config FOREIGN KEY (atividade_config_id) REFERENCES atividade_config(id)
);
