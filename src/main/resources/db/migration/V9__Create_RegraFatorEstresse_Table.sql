-- V9__Create_RegraFatorEstresse_Table.sql

CREATE TABLE regra_fator_estresse (
    id UUID PRIMARY KEY,
    regra_distribuicao_atividade_id UUID NOT NULL,
    peso_multiplicador DOUBLE PRECISION NOT NULL,
    ponto_corte_min DOUBLE PRECISION NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    CONSTRAINT fk_regrafatorestresse_regradistatividade FOREIGN KEY (regra_distribuicao_atividade_id) REFERENCES regra_distribuicao_atividade(id)
);
