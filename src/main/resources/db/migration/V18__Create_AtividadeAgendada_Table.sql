-- V18__Create_AtividadeAgendada_Table.sql

CREATE TABLE atividade_agendada (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    atividade_config_id UUID NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    registro_atividade_id UUID,
    CONSTRAINT fk_agendada_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_agendada_atividade_config FOREIGN KEY (atividade_config_id) REFERENCES atividade_config(id),
    CONSTRAINT fk_agendada_registro_atividade FOREIGN KEY (registro_atividade_id) REFERENCES registro_atividade(id)
);
